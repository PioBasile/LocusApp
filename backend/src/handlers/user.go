package handlers

import (
	"encoding/json"
	"net/http"

	"backend/lib"
	"github.com/golang-jwt/jwt/v5"
	"os"
	"path/filepath"
	"fmt"
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

	var posts []lib.PostResponse
	postQuery := `SELECT id_pub, id_publicateur, description, url_image, date, id_localisation 
				  FROM Publications WHERE id_publicateur = $1`
	err = db.Select(&posts, postQuery, userID)
	if err != nil {
		http.Error(w, "Erreur lors de la récupération des posts", http.StatusInternalServerError)
		return
	}
	profile.Posts = posts

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(profile)
}


func ChangePPHandler(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)
	
	file, handler, err := r.FormFile("profile_picture")
	if err != nil {
		http.Error(w, "Image manquante", http.StatusBadRequest)
		return
	}
	defer file.Close()

	imageName := lib.GenerateNewUUID() + filepath.Ext(handler.Filename)
	dst, err := os.Create("./uploads/profiles_pic/" + imageName)
	if err != nil {
		http.Error(w, "Erreur lors de la création du fichier", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	if _, err := dst.ReadFrom(file); err != nil {
		http.Error(w, "Erreur lors de l'enregistrement de l'image", http.StatusInternalServerError)
		return
	}


	fullImageURL := fmt.Sprintf("%s/uploads/profiles_pic/%s", BaseURL, imageName)

	_, err = db.Exec("UPDATE Utilisateurs SET url_pp = $1 WHERE usr_id = $2", fullImageURL, userID)
	if err != nil {
		http.Error(w, "Erreur lors de la mise à jour du profil", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]string{"message": "Photo de profil mise à jour avec succès !"})
}

func GetUserGroupsHandler(w http.ResponseWriter, r *http.Request) {
    userID := r.Context().Value(UserIDKey).(int)
    
    var groupsID []int 
    query := `SELECT id_grp FROM MembreGroupes WHERE usr_id = $1;`

    err := db.Select(&groupsID, query, userID)
    if err != nil {
        http.Error(w, "Erreur lors de la récupération des IDs", http.StatusInternalServerError)
        return
    }

    if groupsID == nil {
        groupsID = []int{}
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(groupsID)
}

// pour les notifs
func UpdateFCMTokenHandler(w http.ResponseWriter, r *http.Request) {
    userID := r.Context().Value(UserIDKey).(int)
    
    var data struct {
        Token string `json:"fcm_token"`
    }
    
    if err := json.NewDecoder(r.Body).Decode(&data); err != nil {
        http.Error(w, "Données invalides", http.StatusBadRequest)
        return
    }

    query := `UPDATE Utilisateurs SET fcm_token = $1 WHERE usr_id = $2`
    _, err := db.Exec(query, data.Token, userID)
    if err != nil {
        http.Error(w, "Erreur serveur", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
}

func ChangeUsernameHandler(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)

	Username := r.FormValue("username")
	if Username == "" {
		http.Error(w, "Nom d'utilisateur manquant", http.StatusBadRequest)
		return
	}

	query := `UPDATE Utilisateurs SET username = $1 WHERE usr_id = $2`
	_, err := db.Exec(query, Username, userID)
	if err != nil {
		http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]string{"message": "Nom d'utilisateur mis à jour avec succès !"})
}