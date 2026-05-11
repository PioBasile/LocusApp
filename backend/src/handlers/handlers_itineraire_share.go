package handlers

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"time"

	"backend/lib"
)

// ─────────────────────────────────────────────────────────────────────────────
// GET /travelPath/itineraires/share?id=<id_itin>
// Résumé public d'un itinéraire sauvegardé. Endpoint sans auth : toute personne
// avec l'ID peut consulter (URL pensée pour le partage).
// On ne renvoie ni l'usr_id propriétaire en clair ni de champs internes.
// ─────────────────────────────────────────────────────────────────────────────
func ShareItineraireHandler(w http.ResponseWriter, r *http.Request) {
	idStr := strings.TrimSpace(r.URL.Query().Get("id"))
	if idStr == "" {
		http.Error(w, "ID de l'itinéraire manquant", http.StatusBadRequest)
		return
	}
	id, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "ID invalide", http.StatusBadRequest)
		return
	}

	type ShareSummary struct {
		ID            int             `json:"id"`
		Nom           string          `json:"nom"`
		Type          string          `json:"type"`
		Budget        int             `json:"budget"`
		DureeMinutes  int             `json:"duree_minutes"`
		EffortScore   int             `json:"effort_score"`
		Itineraire    json.RawMessage `json:"itineraire"`
		CreatedAt     time.Time       `json:"created_at"`
		LikesCount    int             `json:"likes_count"`
		AuthorName    string          `json:"author_name"`
		PublicURL     string          `json:"public_url"`
	}

	var s ShareSummary
	var donneesStr string
	var authorName sql.NullString
	err = db.QueryRow(`
		SELECT i.id_itin, i.nom, i.type, i.budget, i.duree_min, i.effort, i.donnees, i.created_at,
		       COALESCE(COUNT(l.id_like), 0) AS likes_count,
		       u.username
		FROM Itineraires i
		LEFT JOIN ItinerairesLikes l ON l.id_itin = i.id_itin
		LEFT JOIN Utilisateurs u ON u.usr_id = i.usr_id
		WHERE i.id_itin = $1
		GROUP BY i.id_itin, u.username`, id,
	).Scan(&s.ID, &s.Nom, &s.Type, &s.Budget, &s.DureeMinutes, &s.EffortScore,
		&donneesStr, &s.CreatedAt, &s.LikesCount, &authorName)

	if err == sql.ErrNoRows {
		http.Error(w, "Itinéraire introuvable", http.StatusNotFound)
		return
	} else if err != nil {
		http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		return
	}

	s.Itineraire = json.RawMessage(donneesStr)
	if authorName.Valid {
		s.AuthorName = authorName.String
	} else {
		s.AuthorName = "Anonyme"
	}
	s.PublicURL = fmt.Sprintf("%s/travelPath/itineraires/share?id=%d", BaseURL, s.ID)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(s)
}

// ─────────────────────────────────────────────────────────────────────────────
// GET /travelPath/itineraires/pdf?id=<id_itin>
// Stream un PDF généré côté serveur (texte seul, A4, Helvetica WinAnsi).
// Pas d'auth : permet d'imprimer/exporter à partir d'un lien partagé.
// ─────────────────────────────────────────────────────────────────────────────
func ItinerairePDFHandler(w http.ResponseWriter, r *http.Request) {
	idStr := strings.TrimSpace(r.URL.Query().Get("id"))
	if idStr == "" {
		http.Error(w, "ID de l'itinéraire manquant", http.StatusBadRequest)
		return
	}
	id, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "ID invalide", http.StatusBadRequest)
		return
	}

	var nom, donneesStr string
	var authorName sql.NullString
	err = db.QueryRow(`
		SELECT i.nom, i.donnees, u.username
		FROM Itineraires i
		LEFT JOIN Utilisateurs u ON u.usr_id = i.usr_id
		WHERE i.id_itin = $1`, id).Scan(&nom, &donneesStr, &authorName)
	if err == sql.ErrNoRows {
		http.Error(w, "Itinéraire introuvable", http.StatusNotFound)
		return
	} else if err != nil {
		http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		return
	}

	var itin lib.Itineraire
	if err := json.Unmarshal([]byte(donneesStr), &itin); err != nil {
		http.Error(w, "Données d'itinéraire corrompues", http.StatusInternalServerError)
		return
	}

	pdfBytes := buildItinerairePDF(nom, authorName.String, itin)

	w.Header().Set("Content-Type", "application/pdf")
	w.Header().Set("Content-Disposition", fmt.Sprintf(`inline; filename="itineraire-%d.pdf"`, id))
	w.Header().Set("Content-Length", strconv.Itoa(len(pdfBytes)))
	w.WriteHeader(http.StatusOK)
	w.Write(pdfBytes)
}

func buildItinerairePDF(nom, author string, itin lib.Itineraire) []byte {
	doc := lib.NewPDF()
	doc.NewPage()

	pageH := doc.PageHeight()
	left := 50.0
	y := pageH - 70

	// En-tête
	title := nom
	if title == "" {
		title = itin.Nom
	}
	if title == "" {
		title = "Itinéraire"
	}
	doc.TextBold(left, y, 20, "TravelShare — "+title)
	y -= 24
	if author != "" {
		doc.Text(left, y, 11, "Par "+author)
		y -= 18
	}
	if itin.Resume != "" {
		doc.Text(left, y, 11, truncate(itin.Resume, 110))
		y -= 16
	}

	// Bandeau résumé
	y -= 6
	doc.TextBold(left, y, 12, fmt.Sprintf(
		"Budget : %d€   |   Durée : %d min   |   Effort : %d/5   |   Type : %s",
		itin.BudgetTotal, itin.DureeMinutes, itin.EffortScore, itin.Type,
	))
	y -= 22

	// Séparateur visuel (ligne de tirets)
	doc.Text(left, y, 10, strings.Repeat("—", 56))
	y -= 18

	// Étapes
	for _, e := range itin.Etapes {
		if y < 90 {
			doc.NewPage()
			y = pageH - 70
		}
		doc.TextBold(left, y, 13, fmt.Sprintf("%d. %s  [%s]", e.Ordre, e.NomLieu, e.Creneau))
		y -= 16
		if e.AdresseLieu != "" {
			doc.Text(left, y, 10, truncate(e.AdresseLieu, 110))
			y -= 13
		}
		doc.Text(left, y, 10, fmt.Sprintf(
			"Catégorie : %s   Prix : %d€   Visite : %d min   Trajet : %d min (%.2f km)",
			e.Categorie, e.Prix, e.DureeMinutes, e.TempsTrajMin, e.DistancePrevKm,
		))
		y -= 13
		if e.Horaires != "" {
			doc.Text(left, y, 10, "Horaires : "+truncate(e.Horaires, 100))
			y -= 13
		}
		if e.Note > 0 {
			doc.Text(left, y, 10, fmt.Sprintf("Note : %.1f / 5", e.Note))
			y -= 13
		}
		y -= 6
	}

	// Pied
	if y < 60 {
		doc.NewPage()
		y = pageH - 70
	}
	y -= 10
	doc.Text(left, y, 9, fmt.Sprintf("Généré le %s", time.Now().Format("02/01/2006 15:04")))

	return doc.Bytes()
}

func truncate(s string, max int) string {
	r := []rune(s)
	if len(r) <= max {
		return s
	}
	return string(r[:max-1]) + "…"
}