package handlers

import (
	"database/sql"
	"encoding/json"
	"net/http"

	"backend/lib"
	"golang.org/x/crypto/bcrypt"
)

// SignupHandler creates a new user account with username, email, and password
// Fixes: Uses req.Username instead of hardcoded "NouvelUtilisateur"
func SignupHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Méthode non autorisée", http.StatusMethodNotAllowed)
		return
	}

	var req lib.LoginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "JSON invalide", http.StatusBadRequest)
		return
	}

	if req.Email == "" || req.Password == "" || req.Username == "" {
		http.Error(w, "Email, mot de passe et nom d'utilisateur requis", http.StatusBadRequest)
		return
	}

	hashedPassword, _ := lib.HashPassword(req.Password)

	query := `INSERT INTO Utilisateurs (username, email, password, url_pp) 
              VALUES ($1, $2, $3, $4)`

	_, err := db.Exec(query, req.Username, req.Email, hashedPassword, "img.jpg")
	if err != nil {
		http.Error(w, "Erreur lors de la création (Email déjà utilisé ?)", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]string{"message": "Utilisateur créé en base de données !"})
}

// LoginHandler authenticates a user and returns a JWT token
func LoginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Méthode non autorisée", http.StatusMethodNotAllowed)
		return
	}

	var req lib.LoginRequest
	json.NewDecoder(r.Body).Decode(&req)

	var foundUser lib.User
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

	if err := bcrypt.CompareHashAndPassword([]byte(foundUser.Password), []byte(req.Password)); err != nil {
		http.Error(w, "Email ou mot de passe incorrect", http.StatusUnauthorized)
		return
	}

	tokenString, _ := lib.GenerateJWT(foundUser.ID, foundUser.Username)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(lib.LoginResponse{Token: tokenString})
}
