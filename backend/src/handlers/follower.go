package handlers

import (
	"encoding/json"
	"net/http"
	"backend/lib"
	"strconv"
)

func Follow(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)
	followedID := r.FormValue("user_id")

	err := db.QueryRow("SELECT usr_id FROM Utilisateurs WHERE usr_id = $1", followedID).Scan(&followedID)
	if err != nil {
		http.Error(w, "Utilisateur introuvable", http.StatusNotFound)
		return
	}

	_, err = db.Exec("INSERT INTO Followers (follower_id, followed_id) VALUES ($1, $2)", userID, followedID)
	if err != nil {
		http.Error(w, "Erreur lors du follow.", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]string{"message": "Follow réussie !"})
}

func Unfollow(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)
	followedID := r.FormValue("user_id")

	_, err := db.Exec("DELETE FROM Followers WHERE follower_id = $1 AND followed_id = $2", userID, followedID)
	if err != nil {
		http.Error(w, "Erreur lors du unfollow.", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]string{"message": "Unfollow réussie !"})
}

func GetFollowers(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)
	var followers []lib.FollowerInfo
	query := `SELECT u.usr_id as id, u.username, u.url_pp as ppurl 
			  FROM Followers f 
			  JOIN Utilisateurs u ON f.follower_id = u.usr_id 
			  WHERE f.followed_id = $1`
	err := db.Select(&followers, query, userID)
	if err != nil {
		http.Error(w, "Erreur lors de la récupération des followers", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(followers)
}


func GetTopMostFollowedUsers(w http.ResponseWriter, r *http.Request){

    limitStr := r.FormValue("limit")
    limit := 10 // Valeur par défaut
    if limitStr != "" {
        // SUPPRIMÉ : import "strconv" (il est déjà en haut du fichier)
        if l, err := strconv.Atoi(limitStr); err == nil {
            limit = l
        }
    }

    var followers []lib.FollowerInfo
    // On met le COUNT directement dans le ORDER BY pour ne pas avoir à le stocker
    query := `SELECT u.usr_id as id, u.username, u.url_pp as ppurl
              FROM Utilisateurs u
              LEFT JOIN Followers f ON u.usr_id = f.followed_id
              GROUP BY u.usr_id
              ORDER BY COUNT(f.follower_id) DESC
              LIMIT $1`
    err := db.Select(&followers, query, limit)
    if err != nil {
        http.Error(w, "Erreur lors de la récupération des utilisateurs les plus suivis", http.StatusInternalServerError)
        return
    }
    
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(followers)
}