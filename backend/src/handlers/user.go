package handlers

import (
	"encoding/json"
	"net/http"

	"backend/lib"
	"github.com/golang-jwt/jwt/v5"
)

// GetProfileHandler retrieves the authenticated user's complete profile
func GetProfileHandler(w http.ResponseWriter, r *http.Request) {
	tokenString := r.Header.Get("Authorization")
	token, _ := jwt.Parse(tokenString, func(t *jwt.Token) (interface{}, error) {
		return jwtSecret, nil
	})

	if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
		userID := int(claims["user_id"].(float64))

		var currentUser lib.User
		query := `SELECT usr_id as id, username, email, url_pp as ppurl 
                  FROM Utilisateurs WHERE usr_id = $1`

		err := db.Get(&currentUser, query, userID)
		if err != nil {
			http.Error(w, "Utilisateur introuvable", http.StatusNotFound)
			return
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(currentUser)
	}
}

// GetPublicProfileHandler retrieves a user's public profile information by ID
func GetPublicProfileHandler(w http.ResponseWriter, r *http.Request) {
	userID := r.URL.Query().Get("id")
	if userID == "" {
		http.Error(w, "ID de l'utilisateur manquant", http.StatusBadRequest)
		return
	}

	var profile lib.PublicUserInfo
	query := `SELECT usr_id as id, username, url_pp as ppurl 
			  FROM Utilisateurs WHERE usr_id = $1`
	err := db.Get(&profile, query, userID)
	if err != nil {
		http.Error(w, "Utilisateur introuvable", http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(profile)
}
