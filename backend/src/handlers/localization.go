package handlers

import (
	"database/sql"
	"encoding/json"
	"net/http"
)

// GetLocalisationHandler retrieves location information by ID
func GetLocalisationHandler(w http.ResponseWriter, r *http.Request) {
	locID := r.URL.Query().Get("id")
	if locID == "" {
		http.Error(w, "ID de localisation manquant", http.StatusBadRequest)
		return
	}

	var loc struct {
		ID   int    `db:"id_loc" json:"id"`
		Name string `db:"nom" json:"name"`
		GPS  string `db:"gps" json:"gps"`
	}

	query := `SELECT id_loc, nom, gps FROM Localisation WHERE id_loc = $1`
	err := db.Get(&loc, query, locID)
	if err != nil {
		if err == sql.ErrNoRows {
			http.Error(w, "Localisation introuvable", http.StatusNotFound)
		} else {
			http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		}
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(loc)
}
