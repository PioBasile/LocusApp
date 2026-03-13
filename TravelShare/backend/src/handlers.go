package main

import (
	"encoding/json"
	"net/http"
    "github.com/golang-jwt/jwt/v5"
	"golang.org/x/crypto/bcrypt"
    "database/sql"
)


// LOGIN SIGNUP (Non protégé)


func SignupHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Méthode non autorisée", http.StatusMethodNotAllowed)
		return
	}

	var req LoginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "JSON invalide", http.StatusBadRequest)
		return
	}

	hashedPassword, _ := HashPassword(req.Password)

	query := `INSERT INTO Utilisateurs (username, email, password, url_pp) 
              VALUES ($1, $2, $3, $4)`
	
	_, err := db.Exec(query, "NouvelUtilisateur", req.Email, hashedPassword, "img.jpg")
	if err != nil {
		http.Error(w, "Erreur lors de la création (Email déjà utilisé ?)", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]string{"message": "Utilisateur créé en base de données !"})
}


func LoginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Méthode non autorisée", http.StatusMethodNotAllowed)
		return
	}

	var req LoginRequest
	json.NewDecoder(r.Body).Decode(&req)

	var foundUser User
	query := `SELECT usr_id as id, username, email, password, url_pp as ppurl 
              FROM Utilisateurs WHERE email = $1`
	
	err := db.Get(&foundUser, query, req.Email)
	if err == sql.ErrNoRows {
		http.Error(w, "Email ou mot de passe incorrect", http.StatusUnauthorized)
		return
	} else if err != nil {
		http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		return
	}

	err = bcrypt.CompareHashAndPassword([]byte(foundUser.Password), []byte(req.Password))
	if err != nil {
		http.Error(w, "Email ou mot de passe incorrect", http.StatusUnauthorized)
		return
	}

	tokenString, _ := GenerateJWT(foundUser)
	
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(LoginResponse{Token: tokenString})
}


// Vérificateur des routes protégé

func IsAuthorized(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        tokenString := r.Header.Get("Authorization")
        if tokenString == "" {
            http.Error(w, "Token manquant", http.StatusUnauthorized)
            return
        }

        token, err := jwt.Parse(tokenString, func(t *jwt.Token) (interface{}, error) {
            return jwtSecret, nil
        })

        if err != nil || !token.Valid {
            http.Error(w, "Token invalide", http.StatusUnauthorized)
            return
        }

        next(w, r)
    }
}


// Route protégé

func GetProfileHandler(w http.ResponseWriter, r *http.Request) {
	tokenString := r.Header.Get("Authorization")
	token, _ := jwt.Parse(tokenString, func(t *jwt.Token) (interface{}, error) {
		return jwtSecret, nil
	})

	if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
		userID := int(claims["user_id"].(float64))

		var currentUser User
		// Requête SQL pour récupérer le profil complet
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