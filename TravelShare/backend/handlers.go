package main

import (
	"encoding/json"
	"net/http"
    "github.com/golang-jwt/jwt/v5"
	"golang.org/x/crypto/bcrypt"
)



// LOGIN SIGNUP (Non protégé)


func SignupHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Méthode non autorisée", http.StatusMethodNotAllowed)
        return
    }

    var req LoginRequest 
    json.NewDecoder(r.Body).Decode(&req)

    hashedPassword, _ := HashPassword(req.Password)

    newUser := User{
        ID:       len(usersDB) + 1,
        Email:    req.Email,
        Password: hashedPassword,
        Username: "NouvelUtilisateur",
		ProfilePicture: "img.jpg",
    }
    usersDB = append(usersDB, newUser)

    w.WriteHeader(http.StatusCreated)
    json.NewEncoder(w).Encode(map[string]string{"message": "Utilisateur créé !"})
}


func LoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Méthode non autorisée", http.StatusMethodNotAllowed)
        return
    }

    var req LoginRequest
    err := json.NewDecoder(r.Body).Decode(&req)
    if err != nil {
        http.Error(w, "Données invalides", http.StatusBadRequest)
        return
    }

    var foundUser *User
    for _, u := range usersDB {
        if u.Email == req.Email {
            foundUser = &u
            break
        }
    }

    if foundUser == nil {
        http.Error(w, "Email ou mot de passe incorrect", http.StatusUnauthorized)
        return
    }

    err = bcrypt.CompareHashAndPassword([]byte(foundUser.Password), []byte(req.Password))
    if err != nil {
        http.Error(w, "Email ou mot de passe incorrect", http.StatusUnauthorized)
        return
    }

    tokenString, err := GenerateJWT(*foundUser)
    if err != nil {
        http.Error(w, "Erreur lors de la création du token", http.StatusInternalServerError)
        return
    }
    
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
    // 1. Récupérer le token du header (comme dans le middleware)
    tokenString := r.Header.Get("Authorization")
    
    // 2. Décoder le token (on sait qu'il est valide car le middleware est passé avant)
    token, _ := jwt.Parse(tokenString, func(t *jwt.Token) (interface{}, error) {
        return jwtSecret, nil
    })

    // 3. Extraire les données (Claims)
    if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
        // On récupère le username qu'on a mis dans GenerateJWT
        username := claims["username"].(string)
        userID := int(claims["user_id"].(float64)) // Le JSON transforme les int en float64

        // 4. Chercher les infos complètes dans notre "base de données"
        var currentUser User
        for _, u := range usersDB {
            if u.ID == userID {
                currentUser = u
                break
            }
        }

        // 5. Répondre avec le profil en JSON
        w.Header().Set("Content-Type", "application/json")
        json.NewEncoder(w).Encode(currentUser)
    } else {
        http.Error(w, "Erreur lors de la lecture du profil", http.StatusUnauthorized)
    }
}