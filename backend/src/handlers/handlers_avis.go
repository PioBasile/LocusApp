package handlers
 
import (
	"database/sql"
	"encoding/json"
	"net/http"
	"strconv"
	"strings"
 
	"backend/lib"
)
 
// ─────────────────────────────────────────────────────────────────────────────
// /travelPath/lieux/avis?id=<id_lieu>
//   GET  → liste des avis d'un lieu (public)
//   POST → soumettre un avis (auth requise)
//
// Le path-param `:id` de la spec est exposé comme query param `?id=` pour
// rester cohérent avec les autres endpoints lieux/* (ex: /travelPath/lieu?id=).
// ─────────────────────────────────────────────────────────────────────────────
func LieuxAvisHandler(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodGet:
		getLieuAvis(w, r)
	case http.MethodPost:
		// POST nécessite une auth → on encapsule à la volée.
		IsAuthorized(submitLieuAvis)(w, r)
	default:
		http.Error(w, "Méthode non autorisée", http.StatusMethodNotAllowed)
	}
}
 
func getLieuAvis(w http.ResponseWriter, r *http.Request) {
	idStr := strings.TrimSpace(r.URL.Query().Get("id"))
	if idStr == "" {
		http.Error(w, "ID du lieu manquant", http.StatusBadRequest)
		return
	}
	lieuID, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "ID invalide", http.StatusBadRequest)
		return
	}
 
	rows, err := db.Query(`
		SELECT a.id_avis, a.id_lieu, COALESCE(a.usr_id, -1),
		       COALESCE(u.username, 'Anonyme'),
		       COALESCE(a.note, 0), COALESCE(a.commentaire, ''), a.created_at
		FROM LieuxAvis a
		LEFT JOIN Utilisateurs u ON u.usr_id = a.usr_id
		WHERE a.id_lieu = $1
		ORDER BY a.created_at DESC`, lieuID)
	if err != nil {
		http.Error(w, "Erreur lors de la récupération des avis", http.StatusInternalServerError)
		return
	}
	defer rows.Close()
 
	avis := []lib.LieuAvis{}
	for rows.Next() {
		var a lib.LieuAvis
		if err := rows.Scan(&a.ID, &a.IDLieu, &a.UsrID, &a.Username, &a.Note, &a.Commentaire, &a.CreatedAt); err != nil {
			continue
		}
		avis = append(avis, a)
	}
 
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(avis)
}
 
func submitLieuAvis(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)
 
	idStr := strings.TrimSpace(r.URL.Query().Get("id"))
	if idStr == "" {
		http.Error(w, "ID du lieu manquant", http.StatusBadRequest)
		return
	}
	lieuID, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "ID invalide", http.StatusBadRequest)
		return
	}
 
	var req lib.LieuAvisCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "JSON invalide", http.StatusBadRequest)
		return
	}
	if req.Note < 1 || req.Note > 5 {
		http.Error(w, "Note invalide (1-5 attendu)", http.StatusBadRequest)
		return
	}
 
	// Vérifier que le lieu existe
	var exists int
	if err := db.QueryRow("SELECT 1 FROM Lieux WHERE id_lieu = $1", lieuID).Scan(&exists); err != nil {
		if err == sql.ErrNoRows {
			http.Error(w, "Lieu introuvable", http.StatusNotFound)
			return
		}
		http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		return
	}
 
	// Insertion de l'avis + maj agrégats (note moyenne, nb_avis) dans une transaction.
	tx, err := db.Begin()
	if err != nil {
		http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		return
	}
	defer tx.Rollback()
 
	var avisID int
	err = tx.QueryRow(`
		INSERT INTO LieuxAvis (id_lieu, usr_id, note, commentaire)
		VALUES ($1, $2, $3, $4)
		RETURNING id_avis`,
		lieuID, userID, req.Note, strings.TrimSpace(req.Commentaire),
	).Scan(&avisID)
	if err != nil {
		http.Error(w, "Erreur lors de l'enregistrement de l'avis", http.StatusInternalServerError)
		return
	}
 
	// Recalcul de la note moyenne et du nb_avis
	_, err = tx.Exec(`
		UPDATE Lieux
		SET note = COALESCE((SELECT AVG(note)::float FROM LieuxAvis WHERE id_lieu = $1), 0),
		    nb_avis = (SELECT COUNT(*) FROM LieuxAvis WHERE id_lieu = $1)
		WHERE id_lieu = $1`, lieuID)
	if err != nil {
		http.Error(w, "Erreur lors de la mise à jour des agrégats", http.StatusInternalServerError)
		return
	}
 
	if err := tx.Commit(); err != nil {
		http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		return
	}
 
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"message": "Avis enregistré",
		"id_avis": avisID,
	})
}