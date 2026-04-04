package handlers

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"

	"backend/lib"
)

// MakePostHandler creates a new post with image and text content
func MakePostHandler(w http.ResponseWriter, r *http.Request) {
	
	userID := r.Context().Value(UserIDKey).(int)

	// 2. Parse multipart form (image + text fields)
	err := r.ParseMultipartForm(10 << 20) // 10 MB limit
	if err != nil {
		http.Error(w, "Fichier trop volumineux", http.StatusBadRequest)
		return
	}

	// 3. Extract text fields
	groupe := r.FormValue("groupe")
	description := r.FormValue("description")
	locationID := r.FormValue("id_loc")

	if groupe == "" {
		groupe = "0"
	}

	// 4. Extract and process image file
	file, handler, err := r.FormFile("image")
	if err != nil {
		http.Error(w, "Image manquante", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// 5. Save image with unique name
	imageName := lib.GenerateNewUUID() + filepath.Ext(handler.Filename)
	dst, err := os.Create("./uploads/posts/" + imageName)
	if err != nil {
		http.Error(w, "Erreur lors de la création du fichier", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// 6. Build full image URL
	fullImageURL := fmt.Sprintf("%s/uploads/posts/%s", BaseURL, imageName)

	// 7. Insert post into database
	query := `INSERT INTO Publications (id_publicateur, groupe, description, url_image, id_localisation) 
          	VALUES ($1, $2, $3, $4, $5)`
	_, err = db.Exec(query, userID, groupe, description, fullImageURL, locationID)
	if err != nil {
		http.Error(w, "Erreur SQL lors de l'insertion", http.StatusInternalServerError)
		return
	}

	// 8. Copy image file to disk
	if _, err := io.Copy(dst, file); err != nil {
		http.Error(w, "Erreur lors de l'écriture de l'image", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusCreated)
	fmt.Fprintf(w, "Post créé avec succès ! Image : %s", imageName)
}

// GetPostHandler retrieves a specific post by ID
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

// GetPostPerGroupHandler retrieves all posts for a specific group
func GetPostPerGroupHandler(w http.ResponseWriter, r *http.Request) {
	groupID := r.URL.Query().Get("groupe")

	if groupID == "" {
		http.Error(w, "ID du groupe manquant", http.StatusBadRequest)
		return
	}

	var posts []lib.PostResponse
	query := `SELECT id_pub, id_publicateur, groupe, description, url_image, date, id_localisation
			FROM Publications WHERE groupe = $1`
	err := db.Select(&posts, query, groupID)
	if err != nil {
    	fmt.Println("Erreur getPostsByGroup:", err) 
    	http.Error(w, "Erreur serveur", http.StatusInternalServerError)
    return
}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(posts)
}
