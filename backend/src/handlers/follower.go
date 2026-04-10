package handlers

import (
	"encoding/json"
	"net/http"
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

	rows, err := db.Query("SELECT usr_id, username FROM Utilisateurs WHERE usr_id IN (SELECT follower_id FROM Followers WHERE followed_id = $1)", userID)
	if err != nil {
		http.Error(w, "Erreur lors de la récupération des followers", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var followers []map[string]interface{}
	for rows.Next() {
		var id int
		var username string
		if err := rows.Scan(&id, &username); err != nil {
			http.Error(w, "Erreur lors de la lecture des followers", http.StatusInternalServerError)
			return
		}
		followers = append(followers, map[string]interface{}{
			"id": id,
			"username": username,
		})
	}
	json.NewEncoder(w).Encode(followers)
}