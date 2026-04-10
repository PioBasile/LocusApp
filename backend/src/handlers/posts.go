package handlers

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"backend/lib"
	"time"
	"github.com/jmoiron/sqlx"
)

func MakePostHandler(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)

	// 2. Parse le formulaire multipart (limite de 10 MB pour l'image)
	err := r.ParseMultipartForm(10 << 20) 
	if err != nil {
		http.Error(w, "Fichier trop volumineux", http.StatusBadRequest)
		return
	}

	description := r.FormValue("description")
	locationID := r.FormValue("id_loc")
	groupesRaw := r.MultipartForm.Value["groupe"] 

	if len(groupesRaw) == 0 {
		groupesRaw = []string{"0"}
	}

	var groupIDs []int
	for _, gStr := range groupesRaw {
		groupID, err := strconv.Atoi(gStr)
		if err != nil {
			http.Error(w, "ID de groupe invalide : "+gStr, http.StatusBadRequest)
			return
		}

		if groupID != 0 {
			if !IsMemberOfGroup(userID, groupID) {
				http.Error(w, fmt.Sprintf("Accès refusé : vous n'êtes pas membre du groupe %d", groupID), http.StatusForbidden)
				return
			}
		}
		groupIDs = append(groupIDs, groupID)
	}

	file, handler, err := r.FormFile("image")
	if err != nil {
		http.Error(w, "Image manquante", http.StatusBadRequest)
		return
	}
	defer file.Close()

	imageName := lib.GenerateNewUUID() + filepath.Ext(handler.Filename)
	dstPath := "./uploads/posts/" + imageName
	dst, err := os.Create(dstPath)
	if err != nil {
		http.Error(w, "Erreur lors de la création du fichier", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	fullImageURL := fmt.Sprintf("%s/uploads/posts/%s", BaseURL, imageName)

	tx, err := db.Beginx()
	if err != nil {
		http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		return
	}

	var lastID int
	queryInsertPost := `INSERT INTO Publications (id_publicateur, description, url_image, id_localisation) 
	                    VALUES ($1, $2, $3, $4) RETURNING id_pub`
	
	err = tx.Get(&lastID, queryInsertPost, userID, description, fullImageURL, locationID)
	if err != nil {
		tx.Rollback()
		http.Error(w, "Erreur SQL lors de l'insertion du post", http.StatusInternalServerError)
		return
	}

	queryInsertAssoc := `INSERT INTO PublicationGroupe (id_pub, id_grp) VALUES ($1, $2)`
	for _, gID := range groupIDs {
		_, err = tx.Exec(queryInsertAssoc, lastID, gID)
		if err != nil {
			tx.Rollback()
			http.Error(w, "Erreur SQL lors de l'association du groupe", http.StatusInternalServerError)
			return
		}
	}

	if err = tx.Commit(); err != nil {
		http.Error(w, "Erreur lors de la validation en base de données", http.StatusInternalServerError)
		return
	}

	if _, err := io.Copy(dst, file); err != nil {
		http.Error(w, "Erreur lors de l'écriture de l'image", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusCreated)
	fmt.Fprintf(w, "Post créé avec succès ! Groupes associés : %v", groupIDs)
}

// GetPostHandler retrieves a specific post by ID
func GetPostHandler(w http.ResponseWriter, r *http.Request) {

	fmt.Println("GetPostHandler appelé")
	postID := r.URL.Query().Get("id")
	if postID == "" {
		http.Error(w, "ID du post manquant", http.StatusBadRequest)
		return
	}

	var post struct {
    ID          int     `db:"id_pub" json:"id"`
    UserID      int     `db:"id_publicateur" json:"user_id"`
    Description string  `db:"description" json:"description"`
    ImageURL    string  `db:"url_image" json:"image_url"`
    Groupes     []int   `db:"groupe" json:"groupe"`
    Date        time.Time  `db:"date" json:"date"`
    LocID       *int    `db:"id_localisation" json:"id_loc"`
}

	query := `SELECT id_pub, id_publicateur, description, url_image, date, id_localisation 
              FROM Publications WHERE id_pub = $1`

	err := db.Get(&post, query, postID)
	if err != nil {
		fmt.Println("GetPostHandler erreur db.Get:", err)
		if err == sql.ErrNoRows {
			http.Error(w, "Post introuvable", http.StatusNotFound)
		} else {
			http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		}
		return
	}

	query = `SELECT id_grp FROM PublicationGroupe WHERE id_pub = $1`
	var groups []int
	err = db.Select(&groups, query, postID)
	if err != nil {
		fmt.Println("GetPostHandler erreur db.Select:", err)
		http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		return
	}

	userID := -1
	tokenString := r.Header.Get("Authorization")
	if tokenString != "" {
    	userID = getUserIDFromToken(tokenString)
	}

	authorized := false
	for _, groupID := range groups {
    if IsMemberOfGroup(userID, groupID) {
        authorized = true
        break 
    	}
	}

	if !authorized {
    	http.Error(w, "Accès refusé", http.StatusForbidden)
    	return
	}

	post.Groupes = groups

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(post)
}

func GetPostPerGroupHandler(w http.ResponseWriter, r *http.Request) {
    groupIDStr := r.URL.Query().Get("groupe")
    if groupIDStr == "" {
        http.Error(w, "ID du groupe manquant", http.StatusBadRequest)
        return
    }

    groupID, err := strconv.Atoi(groupIDStr)
    if err != nil {
        http.Error(w, "ID de groupe invalide", http.StatusBadRequest)
        return
    }

    userID := r.Context().Value(UserIDKey).(int)
    if groupID != 0 {
        if !IsMemberOfGroup(userID, groupID) {
            http.Error(w, "Accès refusé : vous n'êtes pas membre de ce groupe", http.StatusForbidden)
            return
        }
    }

    var postIDs []int
    queryIDs := `SELECT id_pub FROM PublicationGroupe WHERE id_grp = $1`
    err = db.Select(&postIDs, queryIDs, groupID)
    if err != nil {
        http.Error(w, "Erreur lors de la récupération des IDs", http.StatusInternalServerError)
        return
    }

    if len(postIDs) == 0 {
        w.Header().Set("Content-Type", "application/json")
        json.NewEncoder(w).Encode([]lib.PostResponse{})
        return
    }

    var posts []lib.PostResponse
    queryPosts, args, err := sqlx.In(`
        SELECT id_pub, id_publicateur, description, url_image, date, id_localisation 
        FROM Publications 
        WHERE id_pub IN (?) 
        ORDER BY date DESC`, postIDs)
    
    if err != nil {
        http.Error(w, "Erreur de préparation SQL", http.StatusInternalServerError)
        return
    }
    
    queryPosts = db.Rebind(queryPosts)
    err = db.Select(&posts, queryPosts, args...)
    if err != nil {
        http.Error(w, "Erreur lors de la récupération des posts", http.StatusInternalServerError)
        return
    }

    for i := range posts {
        var allGroups []int
        queryAllG := `SELECT id_grp FROM PublicationGroupe WHERE id_pub = $1`
        err = db.Select(&allGroups, queryAllG, posts[i].ID)
        if err != nil {
            fmt.Println("Erreur récupération groupes pour post:", posts[i].ID)
            continue
        }
        posts[i].Groupes = allGroups
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(posts)
}