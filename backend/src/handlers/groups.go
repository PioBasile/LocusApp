package handlers

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"backend/lib"
	"path/filepath"
	"os"
	"fmt"
)
func MakeGroupHandler(w http.ResponseWriter, r *http.Request) {


	groupe := r.FormValue("name")
	is_private := r.FormValue("is_private")
	password := r.FormValue("password")
	description := r.FormValue("description")
	file, handler, err := r.FormFile("image")
	
	if groupe == "" {
		http.Error(w, "Le nom du groupe est requis", http.StatusBadRequest)
		return
	}

	if is_private == "true" && password == "" {
		http.Error(w, "Le mot de passe est requis pour un groupe privé", http.StatusBadRequest)
		return
	}

	if err != nil {
		http.Error(w, "Image manquante", http.StatusBadRequest)
		return
	}
	defer file.Close()

	imageName := lib.GenerateNewUUID() + filepath.Ext(handler.Filename)
	dst, err := os.Create("./uploads/groupes_pic/" + imageName)
	if err != nil {
		http.Error(w, "Erreur lors de la création du fichier", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	if _, err := dst.ReadFrom(file); err != nil {
		http.Error(w, "Erreur lors de l'enregistrement de l'image", http.StatusInternalServerError)
		return
	}

	fullImageURL := fmt.Sprintf("%s/uploads/groupes_pic/%s", BaseURL, imageName)

	userID := r.Context().Value(UserIDKey).(int)

	var groupID int
	err = db.QueryRow(
		"INSERT INTO Groupes (nom, is_private, password, owner_id, description, url_image) VALUES ($1, $2, $3, $4, $5, $6) RETURNING id_grp",
		groupe, is_private == "true", password, userID, description, fullImageURL,
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
	rows, err := db.Query("SELECT id_grp, nom, is_private, description, url_image FROM Groupes")
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
		var urlImage sql.NullString 
		if err := rows.Scan(&id, &name, &isPrivate, &description, &urlImage); err != nil {
			http.Error(w, "Erreur lors de la lecture des groupes", http.StatusInternalServerError)
			return
		}
		groups = append(groups, map[string]interface{}{
			"id": id,
			"name": name,
			"is_private": isPrivate,
			"description": description.String,
			"image_url": urlImage.String,
		})
	}

	json.NewEncoder(w).Encode(groups)
}


func JoinGroupHandler(w http.ResponseWriter, r *http.Request) {
	groupID := r.FormValue("group_id")
	password := r.FormValue("password")
	userID := r.Context().Value(UserIDKey).(int)

	var isPrivate bool
	var groupPassword sql.NullString
	err := db.QueryRow("SELECT is_private, password FROM Groupes WHERE id_grp = $1", groupID).Scan(&isPrivate, &groupPassword)
	if err != nil {
		http.Error(w, "Groupe introuvable", http.StatusNotFound)
		return
	}

	if isPrivate && (!groupPassword.Valid || groupPassword.String != password) {
		http.Error(w, "Mot de passe incorrect pour ce groupe privé", http.StatusUnauthorized)
		return
	}

	_, err = db.Exec("INSERT INTO MembreGroupes (id_grp, usr_id) VALUES ($1, $2)", groupID, userID)
	if err != nil {
		http.Error(w, "Erreur lors de l'adhésion au groupe (déjà membre ?)", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]string{"message": "Adhésion au groupe réussie !"})
}


func addPostToGroupHandler(w http.ResponseWriter, r *http.Request) {
	postID := r.FormValue("post_id")
	groupID := r.FormValue("group_id")

	_, err := db.Exec("UPDATE Publications SET groupe = $1 WHERE id_pub = $2", groupID, postID)
	if err != nil {
		http.Error(w, "Erreur lors de l'ajout du post au groupe", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]string{"message": "Post ajouté au groupe avec succès !"})
}


func GetGroupByIDHandler(w http.ResponseWriter, r *http.Request) {
	groupID := r.URL.Query().Get("id")
	if groupID == "" {
		http.Error(w, "L'ID du groupe est requis", http.StatusBadRequest)
		return
	}

	var name string
	var imageURL sql.NullString
	err := db.QueryRow("SELECT nom, url_image FROM Groupes WHERE id_grp = $1", groupID).Scan(&name, &imageURL)
	if err != nil {
		if err == sql.ErrNoRows {
			http.Error(w, "Groupe introuvable", http.StatusNotFound)
		} else {
			http.Error(w, "Erreur lors de la récupération du groupe", http.StatusInternalServerError)
		}
		return
	}

	rows, err := db.Query(`
		SELECT u.usr_id, u.username, u.url_pp 
		FROM Utilisateurs u
		JOIN MembreGroupes mg ON u.usr_id = mg.usr_id
		WHERE mg.id_grp = $1`, groupID)
	if err != nil {
		http.Error(w, "Erreur lors de la récupération des membres", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var members []lib.PublicUserInfo
	for rows.Next() {
		var user lib.PublicUserInfo
		if err := rows.Scan(&user.ID, &user.Username, &user.ProfilePicture); err != nil {
			continue
		}
		members = append(members, user)
	}

	response := map[string]interface{}{
		"name":      name,
		"image_url": imageURL.String,
		"members":   members,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

