package handlers

import (
	"database/sql"
	"encoding/json"
	"net/http"
)
func MakeGroupHandler(w http.ResponseWriter, r *http.Request) {


	groupe := r.FormValue("name")
	is_private := r.FormValue("is_private")
	password := r.FormValue("password")
	description := r.FormValue("description")
	
	if groupe == "" {
		http.Error(w, "Le nom du groupe est requis", http.StatusBadRequest)
		return
	}

	if is_private == "true" && password == "" {
		http.Error(w, "Le mot de passe est requis pour un groupe privé", http.StatusBadRequest)
		return
	}

	userID := r.Context().Value(UserIDKey).(int)

	var groupID int
	err := db.QueryRow(
		"INSERT INTO Groupes (nom, is_private, password, owner_id, description) VALUES ($1, $2, $3, $4, $5) RETURNING id_grp",
		groupe, is_private == "true", password, userID, description,
	).Scan(&groupID)

	if err != nil {
		http.Error(w, "Erreur lors de la création du groupe", http.StatusInternalServerError)
		return
	}

	response := map[string]interface{}{
		"message": "Groupe créé avec succès",
		"group_id": groupID,
	}
	json.NewEncoder(w).Encode(response)
}

func GetGroupsHandler(w http.ResponseWriter, r *http.Request) {
	rows, err := db.Query("SELECT id_grp, nom, is_private, description FROM Groupes")
	if err != nil {
		http.Error(w, "Erreur lors de la récupération des groupes", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var groups []map[string]interface{}
	for rows.Next() {
		var id int
		var name string
		var isPrivate bool
		var description sql.NullString
		if err := rows.Scan(&id, &name, &isPrivate, &description); err != nil {
			http.Error(w, "Erreur lors de la lecture des groupes", http.StatusInternalServerError)
			return
		}
		groups = append(groups, map[string]interface{}{
			"id": id,
			"name": name,
			"is_private": isPrivate,
			"description": description.String,
		})
	}

	json.NewEncoder(w).Encode(groups)
}