package handlers

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"math"
	"net/http"
	"strconv"
	"strings"
	"time"

	"backend/lib"
)

// ─────────────────────────────────────────────────────────────────────────────
// GET /lieux?lat=43.6&lon=3.87&radius_km=5&categorie=restaurant&limit=20
// Retourne les lieux triés par distance (si GPS fourni)
// ─────────────────────────────────────────────────────────────────────────────
func GetLieuxHandler(w http.ResponseWriter, r *http.Request) {
	latStr := r.URL.Query().Get("lat")
	lonStr := r.URL.Query().Get("lon")
	radiusStr := r.URL.Query().Get("radius_km")
	categorie := r.URL.Query().Get("categorie")
	limitStr := r.URL.Query().Get("limit")
	searchQ := r.URL.Query().Get("q")

	limit := 50
	if l, err := strconv.Atoi(limitStr); err == nil && l > 0 && l <= 200 {
		limit = l
	}

	// Construction de la requête dynamique
	query := `SELECT id_lieu, nom, description, adresse, categorie,
	                 gps::text, url_image, note, nb_avis, horaires,
	                 prix_moyen, site_web, telephone, id_loc
	          FROM Lieux WHERE 1=1`
	args := []interface{}{}
	argIdx := 1

	if categorie != "" {
		query += fmt.Sprintf(" AND categorie = $%d", argIdx)
		args = append(args, categorie)
		argIdx++
	}

	if searchQ != "" {
		query += fmt.Sprintf(" AND (nom ILIKE $%d OR description ILIKE $%d OR adresse ILIKE $%d)", argIdx, argIdx, argIdx)
		args = append(args, "%"+searchQ+"%")
		argIdx++
	}

	query += fmt.Sprintf(" LIMIT $%d", argIdx)
	args = append(args, limit)

	var lieux []lib.Lieu
	rows, err := db.Query(query, args...)
	if err != nil {
		http.Error(w, "Erreur lors de la récupération des lieux", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	for rows.Next() {
		var l lib.Lieu
		var urlImage, horaires, siteWeb, telephone sql.NullString
		var idLoc sql.NullInt64
		if err := rows.Scan(
			&l.ID, &l.Nom, &l.Description, &l.Adresse, &l.Categorie,
			&l.GPS, &urlImage, &l.Note, &l.NbAvis, &horaires,
			&l.PrixMoyen, &siteWeb, &telephone, &idLoc,
		); err != nil {
			continue
		}
		if urlImage.Valid { l.URLImage = &urlImage.String }
		if horaires.Valid { l.Horaires = &horaires.String }
		if siteWeb.Valid { l.SiteWeb = &siteWeb.String }
		if telephone.Valid { l.Telephone = &telephone.String }
		if idLoc.Valid { v := int(idLoc.Int64); l.IDLoc = &v }
		lieux = append(lieux, l)
	}

	// Calcul de la distance si GPS fourni
	if latStr != "" && lonStr != "" {
		lat, errLat := strconv.ParseFloat(latStr, 64)
		lon, errLon := strconv.ParseFloat(lonStr, 64)
		if errLat == nil && errLon == nil {
			userGPS := fmt.Sprintf("%f,%f", lat, lon)
			for i := range lieux {
				lieux[i].Distance = lib.CalculateDistance(userGPS, parsePointToCSV(lieux[i].GPS))
			}
			// Tri par distance
			sortLieuxByDistance(lieux)

			// Filtre par rayon si fourni
			if radiusStr != "" {
				if radius, err := strconv.ParseFloat(radiusStr, 64); err == nil {
					filtered := lieux[:0]
					for _, l := range lieux {
						if l.Distance <= radius {
							filtered = append(filtered, l)
						}
					}
					lieux = filtered
				}
			}
		}
	}

	if lieux == nil {
		lieux = []lib.Lieu{}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(lieux)
}

// ─────────────────────────────────────────────────────────────────────────────
// GET /lieu?id=5
// Détail d'un lieu avec photos et posts associés
// ─────────────────────────────────────────────────────────────────────────────
func GetLieuByIDHandler(w http.ResponseWriter, r *http.Request) {
	idStr := r.URL.Query().Get("id")
	if idStr == "" {
		http.Error(w, "ID du lieu manquant", http.StatusBadRequest)
		return
	}

	var l lib.Lieu
	var urlImage, horaires, siteWeb, telephone sql.NullString
	var idLoc sql.NullInt64

	query := `SELECT id_lieu, nom, description, adresse, categorie,
	                 gps::text, url_image, note, nb_avis, horaires,
	                 prix_moyen, site_web, telephone, id_loc
	          FROM Lieux WHERE id_lieu = $1`

	err := db.QueryRow(query, idStr).Scan(
		&l.ID, &l.Nom, &l.Description, &l.Adresse, &l.Categorie,
		&l.GPS, &urlImage, &l.Note, &l.NbAvis, &horaires,
		&l.PrixMoyen, &siteWeb, &telephone, &idLoc,
	)
	if err == sql.ErrNoRows {
		http.Error(w, "Lieu introuvable", http.StatusNotFound)
		return
	} else if err != nil {
		http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		return
	}

	if urlImage.Valid { l.URLImage = &urlImage.String }
	if horaires.Valid { l.Horaires = &horaires.String }
	if siteWeb.Valid { l.SiteWeb = &siteWeb.String }
	if telephone.Valid { l.Telephone = &telephone.String }
	if idLoc.Valid { v := int(idLoc.Int64); l.IDLoc = &v }

	// Chargement des photos
	photoRows, _ := db.Query(`SELECT id_photo, id_lieu, url, COALESCE(legende,''), ordre
	                           FROM LieuxPhotos WHERE id_lieu = $1 ORDER BY ordre`, l.ID)
	if photoRows != nil {
		defer photoRows.Close()
		for photoRows.Next() {
			var p lib.LieuPhoto
			if err := photoRows.Scan(&p.ID, &p.IDLieu, &p.URL, &p.Legende, &p.Ordre); err == nil {
				l.Photos = append(l.Photos, p)
			}
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(l)
}

// ─────────────────────────────────────────────────────────────────────────────
// POST /lieux  (protégé)
// Créer un nouveau lieu
// ─────────────────────────────────────────────────────────────────────────────
func CreateLieuHandler(w http.ResponseWriter, r *http.Request) {
	var req lib.LieuCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "JSON invalide", http.StatusBadRequest)
		return
	}

	if req.Nom == "" || req.Lat == 0 || req.Lon == 0 {
		http.Error(w, "Nom, lat et lon sont requis", http.StatusBadRequest)
		return
	}

	// Valider la catégorie
	cats := map[string]bool{
		"restaurant": true, "bar": true, "cafe": true, "musee": true,
		"monument": true, "parc": true, "shopping": true, "sport": true,
		"hotel": true, "plage": true, "autre": true,
	}
	if req.Categorie == "" {
		req.Categorie = "autre"
	}
	if !cats[req.Categorie] {
		req.Categorie = "autre"
	}

	gpsStr := fmt.Sprintf("(%f,%f)", req.Lat, req.Lon)

	// Créer aussi une entrée Localisation
	var locID int
	err := db.QueryRow(
		"INSERT INTO Localisation (nom, gps) VALUES ($1, $2) RETURNING id_loc",
		req.Nom, gpsStr,
	).Scan(&locID)
	if err != nil {
		http.Error(w, "Erreur lors de la création de la localisation", http.StatusInternalServerError)
		return
	}

	var lieuID int
	err = db.QueryRow(`
		INSERT INTO Lieux (nom, description, adresse, categorie, gps, horaires, prix_moyen, site_web, telephone, id_loc)
		VALUES ($1, $2, $3, $4, $5::point, $6, $7, $8, $9, $10)
		RETURNING id_lieu`,
		req.Nom, req.Description, req.Adresse, req.Categorie, gpsStr,
		nullableStr(req.Horaires), req.PrixMoyen, nullableStr(req.SiteWeb), nullableStr(req.Telephone), locID,
	).Scan(&lieuID)
	if err != nil {
		http.Error(w, "Erreur lors de la création du lieu", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"message": "Lieu créé avec succès",
		"id_lieu": lieuID,
		"id_loc":  locID,
	})
}

// ─────────────────────────────────────────────────────────────────────────────
// GET /searchPosts?q=plage&gps=43.6,3.87&radius_km=10&tags=nature&limit=20
// Recherche de publications (par texte, tags, position)
// ─────────────────────────────────────────────────────────────────────────────
func SearchPostsHandler(w http.ResponseWriter, r *http.Request) {
	query := r.URL.Query().Get("q")
	gps := r.URL.Query().Get("gps")
	radiusStr := r.URL.Query().Get("radius_km")
	tagsStr := r.URL.Query().Get("tags")
	limitStr := r.URL.Query().Get("limit")
	offsetStr := r.URL.Query().Get("offset")

	limit := 20
	if l, err := strconv.Atoi(limitStr); err == nil && l > 0 && l <= 100 {
		limit = l
	}
	offset := 0
	if o, err := strconv.Atoi(offsetStr); err == nil && o >= 0 {
		offset = o
	}

	// Requête SQL de base
	sqlQ := `SELECT p.id_pub, p.id_publicateur, p.description, p.url_image,
	                p.url_audio, p.tags, p.date, p.id_localisation,
	                l.gps::text as loc_gps, l.nom as loc_nom
	         FROM Publications p
	         LEFT JOIN Localisation l ON p.id_localisation = l.id_loc
	         WHERE 1=1`
	args := []interface{}{}
	argIdx := 1

	// Filtre texte
	if query != "" {
		sqlQ += fmt.Sprintf(" AND p.description ILIKE $%d", argIdx)
		args = append(args, "%"+query+"%")
		argIdx++
	}

	// Filtre tags
	if tagsStr != "" {
		tags := strings.Split(tagsStr, ",")
		sqlQ += fmt.Sprintf(" AND p.tags && $%d", argIdx)
		args = append(args, formatTagsArray(tags))
		argIdx++
	}

	sqlQ += " ORDER BY p.date DESC"
	sqlQ += fmt.Sprintf(" LIMIT $%d OFFSET $%d", argIdx, argIdx+1)
	args = append(args, limit, offset)

	type PostSearchResult struct {
		ID          int      `db:"id_pub" json:"id"`
		UserID      int      `db:"id_publicateur" json:"user_id"`
		Description string   `db:"description" json:"description"`
		ImageURL    string   `db:"url_image" json:"image_url"`
		AudioURL    *string  `db:"url_audio" json:"audio_url,omitempty"`
		Tags        []string `json:"tags"`
		Date        time.Time `db:"date" json:"date"`
		IDLoc       *int     `db:"id_localisation" json:"id_loc,omitempty"`
		LocGPS      *string  `json:"loc_gps,omitempty"`
		LocNom      *string  `json:"loc_nom,omitempty"`
		Distance    float64  `json:"distance_km,omitempty"`
	}

	rows, err := db.Query(sqlQ, args...)
	if err != nil {
		http.Error(w, "Erreur lors de la recherche", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var results []PostSearchResult
	for rows.Next() {
		var p PostSearchResult
		var audioURL, locGPS, locNom sql.NullString
		var idLoc sql.NullInt64
		var tagsArr []byte

		if err := rows.Scan(
			&p.ID, &p.UserID, &p.Description, &p.ImageURL,
			&audioURL, &tagsArr, &p.Date, &idLoc, &locGPS, &locNom,
		); err != nil {
			continue
		}

		if audioURL.Valid { p.AudioURL = &audioURL.String }
		if locGPS.Valid { p.LocGPS = &locGPS.String }
		if locNom.Valid { p.LocNom = &locNom.String }
		if idLoc.Valid { v := int(idLoc.Int64); p.IDLoc = &v }

		// Parsing des tags PostgreSQL
		if len(tagsArr) > 0 {
			json.Unmarshal(tagsArr, &p.Tags)
		}

		results = append(results, p)
	}

	// Filtre et tri par GPS si fourni
	if gps != "" && len(results) > 0 {
		radius := 50.0 // km par défaut
		if r, err := strconv.ParseFloat(radiusStr, 64); err == nil && r > 0 {
			radius = r
		}

		filtered := results[:0]
		for i := range results {
			if results[i].LocGPS != nil {
				d := lib.CalculateDistance(gps, parsePointToCSV(*results[i].LocGPS))
				results[i].Distance = d
				if d <= radius {
					filtered = append(filtered, results[i])
				}
			}
		}
		if radiusStr != "" {
			results = filtered
		}
	}

	if results == nil {
		results = []PostSearchResult{}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"total":   len(results),
		"results": results,
	})
}

// ─────────────────────────────────────────────────────────────────────────────
// GET /lieux/:id/posts
// Posts liés à un lieu (via id_localisation)
// ─────────────────────────────────────────────────────────────────────────────
func GetPostsByLieuHandler(w http.ResponseWriter, r *http.Request) {
	lieuID := r.URL.Query().Get("id")
	if lieuID == "" {
		http.Error(w, "ID du lieu manquant", http.StatusBadRequest)
		return
	}

	// Récupérer l'id_loc lié à ce lieu
	var idLoc sql.NullInt64
	err := db.QueryRow("SELECT id_loc FROM Lieux WHERE id_lieu = $1", lieuID).Scan(&idLoc)
	if err != nil {
		http.Error(w, "Lieu introuvable", http.StatusNotFound)
		return
	}

	if !idLoc.Valid {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode([]lib.PostResponse{})
		return
	}

	var posts []lib.PostResponse
	query := `SELECT id_pub, id_publicateur, description, url_image, url_audio, tags, date, id_localisation
	          FROM Publications WHERE id_localisation = $1 ORDER BY date DESC`
	err = db.Select(&posts, query, idLoc.Int64)
	if err != nil {
		http.Error(w, "Erreur lors de la récupération des posts", http.StatusInternalServerError)
		return
	}

	if posts == nil {
		posts = []lib.PostResponse{}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(posts)
}

// ─────────────────────────────────────────────────────────────────────────────
// POST /itineraires/generate
// Génère 3 itinéraires (économique, équilibré, confort)
// ─────────────────────────────────────────────────────────────────────────────
func GenerateItineraireHandler(w http.ResponseWriter, r *http.Request) {
	var req lib.ItineraireRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "JSON invalide", http.StatusBadRequest)
		return
	}

	if req.GPS == "" {
		// GPS par défaut : centre de Montpellier
		req.GPS = "43.6088,3.8783"
	}
	if req.DureeHeures == 0 {
		req.DureeHeures = 4
	}
	if req.BudgetMax == 0 {
		req.BudgetMax = 100
	}

	// 1. Récupérer les lieux candidats depuis la DB
	candidates, err := fetchLieuxCandidates(req)
	if err != nil || len(candidates) == 0 {
		http.Error(w, "Pas assez de lieux disponibles pour générer un itinéraire", http.StatusServiceUnavailable)
		return
	}

	// 2. Générer 3 itinéraires avec des stratégies différentes
	itins := []lib.Itineraire{
		buildItineraire(candidates, req, "economique", 0),
		buildItineraire(candidates, req, "equilibre", 1),
		buildItineraire(candidates, req, "confort", 2),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"itineraires": itins,
		"gps_depart":  req.GPS,
		"duree_heures": req.DureeHeures,
	})
}

// ─────────────────────────────────────────────────────────────────────────────
// POST /itineraires/save  (protégé)
// Sauvegarde un itinéraire pour l'utilisateur
// ─────────────────────────────────────────────────────────────────────────────
func SaveItineraireHandler(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)

	var itin lib.Itineraire
	if err := json.NewDecoder(r.Body).Decode(&itin); err != nil {
		http.Error(w, "JSON invalide", http.StatusBadRequest)
		return
	}

	donnees, _ := json.Marshal(itin)
	var id int
	err := db.QueryRow(`
		INSERT INTO Itineraires (usr_id, nom, type, budget, duree_min, effort, donnees)
		VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING id_itin`,
		userID, itin.Nom, itin.Type, itin.BudgetTotal, itin.DureeMinutes, itin.EffortScore, string(donnees),
	).Scan(&id)
	if err != nil {
		http.Error(w, "Erreur lors de la sauvegarde", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]interface{}{"message": "Itinéraire sauvegardé", "id": id})
}

// ─────────────────────────────────────────────────────────────────────────────
// GET /itineraires  (protégé)
// Récupère les itinéraires sauvegardés de l'utilisateur
// ─────────────────────────────────────────────────────────────────────────────
func GetMyItinerairesHandler(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)

	rows, err := db.Query(`
		SELECT i.id_itin, i.nom, i.type, i.budget, i.duree_min, i.effort, i.donnees, i.created_at,
		       COALESCE(COUNT(l.id_like), 0) AS likes_count
		FROM Itineraires i
		LEFT JOIN ItinerairesLikes l ON l.id_itin = i.id_itin
		WHERE i.usr_id = $1
		GROUP BY i.id_itin
		ORDER BY i.created_at DESC`, userID)
	if err != nil {
		http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	type SavedItin struct {
		ID         int             `json:"id"`
		Nom        string          `json:"nom"`
		Type       string          `json:"type"`
		Budget     int             `json:"budget"`
		DureeMin   int             `json:"duree_minutes"`
		Effort     int             `json:"effort_score"`
		Donnees    json.RawMessage `json:"itineraire"`
		CreatedAt  time.Time       `json:"created_at"`
		LikesCount int             `json:"likes_count"`
	}

	var itins []SavedItin
	for rows.Next() {
		var it SavedItin
		var donneesStr string
		if err := rows.Scan(&it.ID, &it.Nom, &it.Type, &it.Budget, &it.DureeMin, &it.Effort, &donneesStr, &it.CreatedAt, &it.LikesCount); err != nil {
			continue
		}
		it.Donnees = json.RawMessage(donneesStr)
		itins = append(itins, it)
	}

	if itins == nil { itins = []SavedItin{} }

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(itins)
}

// ─────────────────────────────────────────────────────────────────────────────
// ALGORITHME : génération d'itinéraires
// ─────────────────────────────────────────────────────────────────────────────

func fetchLieuxCandidates(req lib.ItineraireRequest) ([]lib.Lieu, error) {
	query := `SELECT id_lieu, nom, description, adresse, categorie,
	                 gps::text, COALESCE(url_image,''), note, nb_avis,
	                 COALESCE(horaires,''), prix_moyen, COALESCE(site_web,''), COALESCE(telephone,'')
	          FROM Lieux`

	if len(req.Categories) > 0 {
		query += " WHERE categorie = ANY($1)"
	}

	var lieux []lib.Lieu
	var rows interface{ Next() bool; Scan(...interface{}) error; Close() error }
	var err error

	if len(req.Categories) > 0 {
		rows, err = db.Query(query, formatCatArray(req.Categories))
	} else {
		rows, err = db.Query(query)
	}

	if err != nil { return nil, err }
	defer rows.Close()

	for rows.Next() {
		var l lib.Lieu
		var urlImg, horaires, siteWeb, tel string
		if err := rows.Scan(
			&l.ID, &l.Nom, &l.Description, &l.Adresse, &l.Categorie,
			&l.GPS, &urlImg, &l.Note, &l.NbAvis, &horaires,
			&l.PrixMoyen, &siteWeb, &tel,
		); err != nil { continue }
		if urlImg != "" { l.URLImage = &urlImg }
		if horaires != "" { l.Horaires = &horaires }
		lieux = append(lieux, l)
	}

	// Calcul et tri par distance depuis le point de départ
	for i := range lieux {
		lieux[i].Distance = lib.CalculateDistance(req.GPS, parsePointToCSV(lieux[i].GPS))
	}
	sortLieuxByDistance(lieux)

	return lieux, nil
}

func buildItineraire(candidates []lib.Lieu, req lib.ItineraireRequest, typeItin string, seed int) lib.Itineraire {
	maxEtapes := durationToSteps(req.DureeHeures)
	budgetMax := req.BudgetMax

	// Stratégie selon le type
	var prixMax int
	var preferNote bool
	switch typeItin {
	case "economique":
		prixMax = 15
		preferNote = false
		budgetMax = min(budgetMax, 30)
	case "equilibre":
		prixMax = 40
		preferNote = true
		budgetMax = min(budgetMax, 80)
	case "confort":
		prixMax = 999
		preferNote = true
	}

	// Plan des créneaux selon le nombre d'étapes (matin → après-midi → soir, puis on boucle)
	creneauxPlan := buildCreneauxPlan(maxEtapes)

	// Sélection des lieux (créneau-aware + favoris obligatoires).
	// On récupère aussi les créneaux effectivement retenus, certains pouvant être vides.
	selected, creneauxRetenus := selectLieux(candidates, creneauxPlan, prixMax, preferNote, budgetMax, seed, req.LieuxFavoris)

	// Construction des étapes
	var etapes []lib.ItineraireEtape
	budgetTotal := 0
	dureeTotal := 0
	prevGPS := req.GPS

	for i, l := range selected {
		distPrev := lib.CalculateDistance(prevGPS, parsePointToCSV(l.GPS))
		tempsTraj := int(distPrev / 4.0 * 60) // ~4 km/h à pied
		if distPrev > 1 { tempsTraj = int(distPrev / 30.0 * 60) } // ~30 km/h en transport

		dureeVisite := getDureeVisite(l.Categorie)
		creneau := creneauxRetenus[i]

		imgURL := ""
		if l.URLImage != nil { imgURL = *l.URLImage }
		horaires := ""
		if l.Horaires != nil { horaires = *l.Horaires }

		etape := lib.ItineraireEtape{
			Ordre:          i + 1,
			IDLieu:         l.ID,
			NomLieu:        l.Nom,
			AdresseLieu:    l.Adresse,
			GPSLieu:        l.GPS,
			Categorie:      l.Categorie,
			Creneau:        creneau,
			DureeMinutes:   dureeVisite,
			DistancePrevKm: math.Round(distPrev*100) / 100,
			TempsTrajMin:   tempsTraj,
			Prix:           l.PrixMoyen,
			Horaires:       horaires,
			URLImage:       imgURL,
			Note:           l.Note,
		}

		etapes = append(etapes, etape)
		budgetTotal += l.PrixMoyen
		dureeTotal += dureeVisite + tempsTraj
		prevGPS = parsePointToCSV(l.GPS)
	}

	nomTypeMap := map[string]string{
		"economique": "Circuit Économique",
		"equilibre":  "Parcours Équilibré",
		"confort":    "Expérience Confort",
	}
	resumeMap := map[string]string{
		"economique": "Un itinéraire complet à petit prix, en profitant des trésors gratuits ou abordables de Montpellier.",
		"equilibre":  "Le parfait équilibre entre découverte culturelle, gastronomie locale et moments de détente.",
		"confort":    "Une expérience premium : les meilleures tables, activités d'exception et adresses incontournables.",
	}
	effortMap := map[string]int{"economique": 3, "equilibre": 2, "confort": 1}

	return lib.Itineraire{
		ID:            seed + 1,
		Type:          typeItin,
		Nom:           nomTypeMap[typeItin],
		Etapes:        etapes,
		BudgetTotal:   budgetTotal,
		DureeMinutes:  dureeTotal,
		EffortScore:   effortMap[typeItin],
		MeteoSensible: typeItin == "economique",
		Resume:        resumeMap[typeItin],
	}
}

// buildCreneauxPlan construit la séquence de créneaux pour n étapes.
// Heuristique : on commence le matin, puis après-midi, puis soir, en cyclant si besoin.
func buildCreneauxPlan(n int) []string {
	base := []string{"matin", "apres-midi", "soir"}
	out := make([]string, n)
	for i := 0; i < n; i++ {
		out[i] = base[i%len(base)]
	}
	return out
}

// selectLieux sélectionne un lieu par créneau, en respectant les favoris obligatoires
// et en filtrant sur les horaires d'ouverture du lieu pour le créneau visé.
//
// Retourne deux slices parallèles : les lieux retenus et les créneaux effectivement
// assignés (utile car certains créneaux peuvent rester vides et être omis).
//
// Stratégie :
//  1. Passe favoris : pour chaque ID dans lieuxFavoris, on place le lieu sur le 1er créneau
//     compatible (horaires + non-doublon) — même s'il dépasse prixMax (mandatory).
//     Le budget total reste contraint par budgetMax (on saute un favori qui le fait exploser).
//  2. Passe principale : pour chaque créneau non rempli, on prend le meilleur candidat
//     compatible (horaires, budget, prix max, éviter deux restos consécutifs).
//  3. Passe de secours : si un créneau reste vide, on relâche la contrainte horaires.
func selectLieux(candidates []lib.Lieu, creneaux []string, prixMax int, preferNote bool,
	budgetMax, seed int, lieuxFavoris []string) ([]lib.Lieu, []string) {

	n := len(creneaux)
	if n == 0 {
		return nil, nil
	}

	// Index rapide des candidats par ID
	byID := make(map[int]lib.Lieu, len(candidates))
	for _, l := range candidates {
		byID[l.ID] = l
	}

	// IDs favoris (parse string → int, on ignore silencieusement les valeurs invalides)
	favIDs := make([]int, 0, len(lieuxFavoris))
	for _, s := range lieuxFavoris {
		if id, err := strconv.Atoi(strings.TrimSpace(s)); err == nil {
			favIDs = append(favIDs, id)
		}
	}

	result := make([]lib.Lieu, n)
	filled := make([]bool, n)
	seen := map[int]bool{}
	budgetUsed := 0

	// Passe 1 : favoris obligatoires
	for _, id := range favIDs {
		l, ok := byID[id]
		if !ok || seen[id] {
			continue
		}
		// Trouver le 1er créneau compatible non rempli
		placed := false
		for i := 0; i < n; i++ {
			if filled[i] {
				continue
			}
			if !lieuOuvertCreneau(strOrEmpty(l.Horaires), creneaux[i]) {
				continue
			}
			if budgetUsed+l.PrixMoyen > budgetMax {
				continue
			}
			result[i] = l
			filled[i] = true
			seen[id] = true
			budgetUsed += l.PrixMoyen
			placed = true
			break
		}
		// Si rien de compatible côté horaires, on tente sans contrainte horaires
		// (le favori est mandatory)
		if !placed {
			for i := 0; i < n; i++ {
				if filled[i] {
					continue
				}
				if budgetUsed+l.PrixMoyen > budgetMax {
					continue
				}
				result[i] = l
				filled[i] = true
				seen[id] = true
				budgetUsed += l.PrixMoyen
				break
			}
		}
	}

	// Passe 2 : compléter les créneaux vides avec les meilleurs candidats compatibles
	// Décalage selon le seed pour différencier les 3 itinéraires
	offset := seed * 3
	if len(candidates) == 0 {
		return compactLieuxAndCreneaux(result, filled, creneaux)
	}

	tryFill := func(strict bool) {
		for i := 0; i < n; i++ {
			if filled[i] {
				continue
			}
			creneau := creneaux[i]
			for k := 0; k < len(candidates); k++ {
				l := candidates[(k+offset)%len(candidates)]
				if seen[l.ID] {
					continue
				}
				if l.PrixMoyen > prixMax {
					continue
				}
				if budgetUsed+l.PrixMoyen > budgetMax {
					continue
				}
				// Éviter deux restaurants consécutifs
				if i > 0 && filled[i-1] && result[i-1].Categorie == "restaurant" && l.Categorie == "restaurant" {
					continue
				}
				if strict && !lieuOuvertCreneau(strOrEmpty(l.Horaires), creneau) {
					continue
				}
				result[i] = l
				filled[i] = true
				seen[l.ID] = true
				budgetUsed += l.PrixMoyen
				break
			}
		}
	}

	tryFill(true)  // d'abord avec filtre horaires
	tryFill(false) // assouplissement si certains créneaux restent vides

	outLieux, outCreneaux := compactLieuxAndCreneaux(result, filled, creneaux)

	// preferNote : tri secondaire par note décroissante des lieux non-favoris.
	// On garde les favoris à leur place pour ne pas casser leur attribution de créneau.
	if preferNote && len(outLieux) > 1 {
		favSet := map[int]bool{}
		for _, id := range favIDs {
			favSet[id] = true
		}
		// Tri par insertion adjacent — note décroissante, sans déplacer les favoris.
		// Les créneaux sont déplacés en parallèle pour rester cohérents.
		for i := 1; i < len(outLieux); i++ {
			for j := i; j > 0; j-- {
				if favSet[outLieux[j].ID] || favSet[outLieux[j-1].ID] {
					break
				}
				if outLieux[j].Note > outLieux[j-1].Note {
					outLieux[j], outLieux[j-1] = outLieux[j-1], outLieux[j]
					outCreneaux[j], outCreneaux[j-1] = outCreneaux[j-1], outCreneaux[j]
				} else {
					break
				}
			}
		}
	}

	return outLieux, outCreneaux
}

func compactLieuxAndCreneaux(arr []lib.Lieu, filled []bool, creneaux []string) ([]lib.Lieu, []string) {
	outL := make([]lib.Lieu, 0, len(arr))
	outC := make([]string, 0, len(arr))
	for i, l := range arr {
		if filled[i] {
			outL = append(outL, l)
			outC = append(outC, creneaux[i])
		}
	}
	return outL, outC
}

func strOrEmpty(p *string) string {
	if p == nil {
		return ""
	}
	return *p
}

// ─────────────────────────────────────────────────────────────────────────────
// Parsing des horaires : décide si un lieu est ouvert pendant un créneau donné.
//
// Le champ `horaires` est du texte libre, ex :
//   "Lun-Sam 12h-14h / 19h-22h"
//   "Tous les jours 18h-minuit"
//   "Accès libre 24h/24"
//   "Selon programmation"
//
// On extrait toutes les plages "Xh-Yh" / "XhMM-YhMM" et on vérifie l'intersection
// avec la fenêtre du créneau. Si on ne trouve aucune plage parsable, on considère
// le lieu comme ouvert (cas "accès libre", "selon programmation", etc.).
// ─────────────────────────────────────────────────────────────────────────────

// creneauWindow retourne (heureDébut, heureFin) en heures décimales pour un créneau.
func creneauWindow(creneau string) (float64, float64) {
	switch creneau {
	case "matin":
		return 8, 12
	case "apres-midi", "après-midi":
		return 12, 18
	case "soir":
		return 18, 23
	default:
		return 0, 24
	}
}

// parseHoraireRanges extrait les plages horaires d'un texte libre français.
// Retourne une liste de paires (début, fin) en heures décimales (ex: 19.5 = 19h30).
func parseHoraireRanges(s string) [][2]float64 {
	if s == "" {
		return nil
	}
	low := strings.ToLower(s)

	// Cas spéciaux : ouvert tout le temps / inconnu → ouvert toute la journée
	openAllDay := []string{"24h/24", "24/24", "accès libre", "acces libre", "selon programmation", "selon evenement", "selon événement"}
	for _, kw := range openAllDay {
		if strings.Contains(low, kw) {
			return [][2]float64{{0, 24}}
		}
	}

	// "minuit" → 24h pour le calcul d'intersection
	low = strings.ReplaceAll(low, "minuit", "24h")

	var ranges [][2]float64
	// Découpe sur séparateurs courants entre plages
	for _, part := range strings.FieldsFunc(low, func(r rune) bool {
		return r == '/' || r == ',' || r == ';'
	}) {
		// Cherche un motif "Xh[MM]-Yh[MM]" dans la partie
		if r := extractRange(part); r != nil {
			ranges = append(ranges, *r)
		}
	}
	return ranges
}

// extractRange cherche la 1ère occurrence d'une plage "Xh[MM]-Yh[MM]" dans une portion de texte.
func extractRange(s string) *[2]float64 {
	// On scanne caractère par caractère, en cherchant "<digits>h<digits?>-<digits>h<digits?>"
	runes := []rune(s)
	i := 0
	for i < len(runes) {
		// début : digit
		if !isDigit(runes[i]) {
			i++
			continue
		}
		// Tentative de match
		start, ok1, next := readTimeToken(runes, i)
		if !ok1 {
			i++
			continue
		}
		// skip séparateurs "-", "à", "to", "-"
		j := next
		for j < len(runes) && (runes[j] == ' ' || runes[j] == '-' || runes[j] == '\u00e0' /* à */) {
			j++
		}
		// chercher "a " optionnel
		if j+1 < len(runes) && runes[j] == 'a' && runes[j+1] == ' ' {
			j += 2
		}
		if j >= len(runes) || !isDigit(runes[j]) {
			i = next
			continue
		}
		end, ok2, after := readTimeToken(runes, j)
		if !ok2 {
			i = next
			continue
		}
		// Plage valide
		if end < start {
			// ex : 22h-2h (overnight) → on coupe à 24h pour simplifier
			end = 24
		}
		_ = after
		return &[2]float64{start, end}
	}
	return nil
}

// readTimeToken lit un token type "12h" ou "12h30" depuis runes[i:], retourne (heure, ok, nextIdx).
func readTimeToken(runes []rune, i int) (float64, bool, int) {
	start := i
	for i < len(runes) && isDigit(runes[i]) {
		i++
	}
	if i == start {
		return 0, false, start
	}
	hStr := string(runes[start:i])
	h, err := strconv.Atoi(hStr)
	if err != nil || h < 0 || h > 30 {
		return 0, false, start
	}
	// 'h' requis
	if i >= len(runes) || (runes[i] != 'h' && runes[i] != 'H') {
		return 0, false, start
	}
	i++
	// minutes optionnelles
	mStart := i
	for i < len(runes) && isDigit(runes[i]) && i-mStart < 2 {
		i++
	}
	mins := 0
	if i > mStart {
		mins, _ = strconv.Atoi(string(runes[mStart:i]))
	}
	return float64(h) + float64(mins)/60.0, true, i
}

func isDigit(r rune) bool { return r >= '0' && r <= '9' }

// lieuOuvertCreneau retourne true si le lieu est ouvert (au moins partiellement)
// pendant la fenêtre du créneau demandé. Pas d'info exploitable → true (permissif).
func lieuOuvertCreneau(horaires, creneau string) bool {
	if strings.TrimSpace(horaires) == "" {
		return true
	}
	ranges := parseHoraireRanges(horaires)
	if len(ranges) == 0 {
		return true
	}
	cStart, cEnd := creneauWindow(creneau)
	for _, r := range ranges {
		// intersection non vide : r[0] < cEnd ET r[1] > cStart
		if r[0] < cEnd && r[1] > cStart {
			return true
		}
	}
	return false
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

// parsePointToCSV convertit "(lat,lon)" en "lat,lon"
func parsePointToCSV(gpsPoint string) string {
	s := strings.TrimSpace(gpsPoint)
	s = strings.TrimPrefix(s, "(")
	s = strings.TrimSuffix(s, ")")
	return s
}

func sortLieuxByDistance(lieux []lib.Lieu) {
	for i := 1; i < len(lieux); i++ {
		for j := i; j > 0 && lieux[j].Distance < lieux[j-1].Distance; j-- {
			lieux[j], lieux[j-1] = lieux[j-1], lieux[j]
		}
	}
}

func durationToSteps(heures int) int {
	switch {
	case heures <= 2: return 2
	case heures <= 4: return 3
	case heures <= 6: return 4
	case heures <= 8: return 5
	default: return 6
	}
}

func getDureeVisite(categorie string) int {
	switch categorie {
	case "musee": return 90
	case "restaurant": return 75
	case "monument": return 45
	case "parc": return 60
	case "cafe": return 30
	case "bar": return 60
	case "shopping": return 60
	case "sport": return 90
	default: return 45
	}
}

func nullableStr(s string) interface{} {
	if s == "" { return nil }
	return s
}

func formatTagsArray(tags []string) string {
	return "{" + strings.Join(tags, ",") + "}"
}

func formatCatArray(cats []string) string {
	quoted := make([]string, len(cats))
	for i, c := range cats { quoted[i] = c }
	return "{" + strings.Join(quoted, ",") + "}"
}

func min(a, b int) int {
	if a < b { return a }
	return b
}


func LikeItineraireHandler(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)
	itinID := r.URL.Query().Get("id")

	query := `INSERT INTO ItinerairesLikes (usr_id, id_itin) VALUES ($1, $2)
	          ON CONFLICT DO NOTHING` // Évite les doublons
	_, err := db.Exec(query, userID, itinID)
	if err != nil {
		http.Error(w, "Erreur lors du like", http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]string{"message": "Itinéraire liké"})
}

// DELETE /itineraires/unlike?id=1
func UnlikeItineraireHandler(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)
	itinID := r.URL.Query().Get("id")

	query := `DELETE FROM ItinerairesLikes WHERE usr_id = $1 AND id_itin = $2`
	_, err := db.Exec(query, userID, itinID)
	if err != nil {
		http.Error(w, "Erreur lors du unlike", http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusOK)
}

func SearchItinerairesHandler(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)
	
	// Récupération des paramètres
	catsStr := r.URL.Query().Get("categories")
	queryParam := r.URL.Query().Get("q")

	// Construction de la requête SQL
	// Nous utilisons ILIKE sur la colonne 'donnees' qui contient le JSON complet de l'itinéraire
	sqlQ := `SELECT i.id_itin, i.nom, i.type, i.budget, i.duree_min, i.effort, i.donnees, i.created_at,
	                COALESCE(COUNT(l.id_like), 0) AS likes_count
	         FROM Itineraires i
	         LEFT JOIN ItinerairesLikes l ON l.id_itin = i.id_itin
	         WHERE i.usr_id = $1`
	
	args := []interface{}{userID}
	argIdx := 2

	// Filtrage par catégories (recherche dans le contenu JSON)
	if catsStr != "" {
		cats := strings.Split(catsStr, ",")
		for _, cat := range cats {
			sqlQ += fmt.Sprintf(" AND i.donnees::text ILIKE $%d", argIdx)
			args = append(args, "%"+strings.TrimSpace(cat)+"%")
			argIdx++
		}
	}

	// Filtrage par nom d'itinéraire ou description de lieu
	if queryParam != "" {
		sqlQ += fmt.Sprintf(" AND (i.nom ILIKE $%d OR i.donnees::text ILIKE $%d)", argIdx, argIdx)
		args = append(args, "%"+queryParam+"%")
		argIdx++
	}

	sqlQ += " GROUP BY i.id_itin ORDER BY i.created_at DESC"

	rows, err := db.Query(sqlQ, args...)
	if err != nil {
		http.Error(w, "Erreur lors de la recherche d'itinéraires", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	type SavedItin struct {
		ID         int             `json:"id"`
		Nom        string          `json:"nom"`
		Type       string          `json:"type"`
		Budget     int             `json:"budget"`
		DureeMin   int             `json:"duree_minutes"`
		Effort     int             `json:"effort_score"`
		Donnees    json.RawMessage `json:"itineraire"`
		CreatedAt  time.Time       `json:"created_at"`
		LikesCount int             `json:"likes_count"`
	}

	var itins []SavedItin
	for rows.Next() {
		var it SavedItin
		var donneesStr string
		if err := rows.Scan(&it.ID, &it.Nom, &it.Type, &it.Budget, &it.DureeMin, &it.Effort, &donneesStr, &it.CreatedAt, &it.LikesCount); err != nil {
			continue
		}
		it.Donnees = json.RawMessage(donneesStr)
		itins = append(itins, it)
	}

	if itins == nil {
		itins = []SavedItin{}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(itins)
}