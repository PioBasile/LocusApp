package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"github.com/golang-jwt/jwt/v5"
	"golang.org/x/crypto/bcrypt"
)

const BaseURL = "http://localhost:8080"

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

func MakePostHandler(w http.ResponseWriter, r *http.Request) {
    // 1. Authentification (déjà fonctionnelle)
    tokenString := r.Header.Get("Authorization")
    token, _ := jwt.Parse(tokenString, func(t *jwt.Token) (interface{}, error) {
        return jwtSecret, nil
    })
    claims, _ := token.Claims.(jwt.MapClaims)
    userID := int(claims["user_id"].(float64))

    // 2. Parser le formulaire (Image + Texte)
    err := r.ParseMultipartForm(10 << 20)
    if err != nil {
        http.Error(w, "Fichier trop volumineux", http.StatusBadRequest)
        return
    }

    // 3. Récupérer les champs TEXTE via FormValue (au lieu du JSON)
    groupe := r.FormValue("groupe")
    description := r.FormValue("description")
	locationID := r.FormValue("id_loc")

    if groupe == "" {
        groupe = "0" 
    }

    // 4. Récupérer le FICHIER image
    file, handler, err := r.FormFile("image")
    if err != nil {
        http.Error(w, "Image manquante", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // 5. Préparer le fichier de destination
    imageName := GenerateNewUUID() + filepath.Ext(handler.Filename)
    dst, err := os.Create("./uploads/posts/" + imageName)
    if err != nil {
        http.Error(w, "Erreur lors de la création du fichier", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

	fullImageURL := fmt.Sprintf("%s/uploads/posts/%s", BaseURL, imageName)

    // 6. Sauvegarder en Base de Données
	query := `INSERT INTO Publications (id_publicateur, groupes, description, url_image, id_localisation) 
          	VALUES ($1, $2, $3, $4, $5)`
    _, err = db.Exec(query, userID, groupe, description, fullImageURL, locationID)

    if err != nil {
        http.Error(w, "Erreur SQL lors de l'insertion", http.StatusInternalServerError)
        return
    }

    // 7. Copier les données de l'image sur le disque
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "Erreur lors de l'écriture de l'image", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "Post créé avec succès ! Image : %s", imageName)
}



// Dans src/handlers.go

func GetPostHandler(w http.ResponseWriter, r *http.Request) {

    postID := r.URL.Query().Get("id")
    if postID == "" {
        http.Error(w, "ID du post manquant", http.StatusBadRequest)
        return
    }

    var post struct {
        ID          int    `db:"id_pub" json:"id"`
        UserID      int    `db:"id_publicateur" json:"user_id"`
        Groupe      int    `db:"groupe" json:"groupe"`
        Description string `db:"description" json:"description"`
        ImageURL    string `db:"url_image" json:"image_url"`
        Date        string `db:"date" json:"date"`
        LocID       *int   `db:"id_localisation" json:"id_loc"` 
    }

    query := `SELECT id_pub, id_publicateur, groupe, description, url_image, date, id_localisation 
              FROM Publications WHERE id_pub = $1`
    
    err := db.Get(&post, query, postID)
    if err != nil {
        if err == sql.ErrNoRows {
            http.Error(w, "Post introuvable", http.StatusNotFound)
        } else {
            http.Error(w, "Erreur serveur", http.StatusInternalServerError)
        }
        return
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(post)
}


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


func GetPublicProfileHandler(w http.ResponseWriter, r *http.Request) {
	userID := r.URL.Query().Get("id")
	if userID == "" {
		http.Error(w, "ID de l'utilisateur manquant", http.StatusBadRequest)
		return
	}
	
	var profile PublicUserInfo
	query := `SELECT usr_id as id, username, url_pp as ppurl 
			  FROM Utilisateurs WHERE usr_id = $1`
	err := db.Get(&profile, query, userID)
	if err != nil {
		if err == sql.ErrNoRows {
			http.Error(w, "Utilisateur introuvable", http.StatusNotFound)
		} else {
			http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		}
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(profile)
}