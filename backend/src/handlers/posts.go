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
	"github.com/lib/pq"
)

func MakePostHandler(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)

	// 1. Parse le formulaire multipart (limite augmentée à 50 MB pour image + audio)
	err := r.ParseMultipartForm(50 << 20)
	if err != nil {
		http.Error(w, "Fichiers trop volumineux", http.StatusBadRequest)
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

	// --- GESTION DE L'IMAGE (OBLIGATOIRE) ---
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
		http.Error(w, "Erreur lors de la création du fichier image", http.StatusInternalServerError)
		return
	}
	defer dst.Close()
	
	fullImageURL := fmt.Sprintf("%s/uploads/posts/%s", BaseURL, imageName)

	// --- GESTION DE L'AUDIO (OPTIONNEL) ---
	var audioURL sql.NullString // Permet d'insérer NULL proprement en DB si aucun audio n'est fourni

	audioFile, audioHandler, errAudio := r.FormFile("audio")
	if errAudio == nil {
		// Si un fichier audio est présent
		defer audioFile.Close()

		audioName := lib.GenerateNewUUID() + filepath.Ext(audioHandler.Filename)
		audioDstPath := "./uploads/posts_audio/" + audioName
		audioDst, err := os.Create(audioDstPath)
		if err != nil {
			http.Error(w, "Erreur lors de la création du fichier audio", http.StatusInternalServerError)
			return
		}
		defer audioDst.Close()

		// On copie directement le fichier audio
		if _, err := io.Copy(audioDst, audioFile); err != nil {
			http.Error(w, "Erreur lors de l'écriture de l'audio", http.StatusInternalServerError)
			return
		}

		// On valide la variable sql.NullString
		audioURL = sql.NullString{
			String: fmt.Sprintf("%s/uploads/posts_audio/%s", BaseURL, audioName),
			Valid:  true,
		}
	} else if errAudio != http.ErrMissingFile {
		// S'il y a une erreur autre que "fichier manquant" (ex: fichier corrompu)
		http.Error(w, "Erreur lors de la lecture du fichier audio", http.StatusBadRequest)
		return
	}

	// --- SAUVEGARDE EN BASE DE DONNÉES ---
	tx, err := db.Beginx()
	if err != nil {
		http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		return
	}

	var lastID int
	// Ajout de url_audio dans la requête SQL
	queryInsertPost := `INSERT INTO Publications (id_publicateur, description, url_image, url_audio, id_localisation) 
	                    VALUES ($1, $2, $3, $4, $5) RETURNING id_pub`

	// On passe audioURL comme 4ème paramètre
	err = tx.Get(&lastID, queryInsertPost, userID, description, fullImageURL, audioURL, locationID)
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


	// On copie l'image uniquement si la transaction SQL a réussi
	if _, err := io.Copy(dst, file); err != nil {
		http.Error(w, "Erreur lors de l'écriture de l'image", http.StatusInternalServerError)
		return
	}

	// Notification push
	go NotifyGroupMembersPush(groupIDs, userID, description)

	// Save manual tags if provided; only run AI when no manual tags were sent
	tagsRaw := r.MultipartForm.Value["tags"]
	if len(tagsRaw) > 0 {
		_, _ = db.Exec(`UPDATE Publications SET tags = $1 WHERE id_pub = $2`, pq.StringArray(tagsRaw), lastID)
	} else if r.FormValue("ai_tags") != "false" {
		go GenerateAndSaveTags(lastID, description, dstPath)
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
		ID          int            `db:"id_pub" json:"id"`
		UserID      int            `db:"id_publicateur" json:"user_id"`
		Description string         `db:"description" json:"description"`
		ImageURL    string         `db:"url_image" json:"image_url"`
		Groupes     []int          `db:"groupe" json:"groupe"`
		Date        time.Time      `db:"date" json:"date"`
		LocID       *int           `db:"id_localisation" json:"id_loc"`
		AudioURL    *string        `db:"url_audio" json:"audio_url,omitempty"`
		Tags        pq.StringArray `db:"tags" json:"tags"` // <--- AJOUT ICI
	}

	query := `SELECT id_pub, id_publicateur, description, url_image, date, id_localisation, url_audio, tags
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
		SELECT id_pub, id_publicateur, description, url_image, url_audio, tags, date, id_localisation 
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

func DeletePostHandler(w http.ResponseWriter, r *http.Request) {
	postID := r.URL.Query().Get("id")
	if postID == "" {
		http.Error(w, "ID du post manquant", http.StatusBadRequest)
		return
	}

	userID := r.Context().Value(UserIDKey).(int)

	var postOwnerID int
	queryOwner := `SELECT id_publicateur FROM Publications WHERE id_pub = $1`
	err := db.Get(&postOwnerID, queryOwner, postID)
	if err != nil {
		if err == sql.ErrNoRows {
			http.Error(w, "Post introuvable", http.StatusNotFound)
		} else {
			http.Error(w, "Erreur serveur", http.StatusInternalServerError)
		}
		return
	}

	if postOwnerID != userID {
		http.Error(w, "Accès refusé : vous n'êtes pas le propriétaire du post", http.StatusForbidden)
		return
	}

	query := `DELETE FROM Publications WHERE id_pub = $1`
	result, err := db.Exec(query, postID)
	if err != nil {
		http.Error(w, "Erreur lors de la suppression du post", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		http.Error(w, "Erreur lors de la vérification de la suppression", http.StatusInternalServerError)
		return
	}

	if rowsAffected == 0 {
		http.Error(w, "Post introuvable", http.StatusNotFound)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintln(w, "Post supprimé avec succès")
}

// REPORTS

func ReportPostHandler(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)
	postID := r.URL.Query().Get("id")
	comment := r.FormValue("comment")
	reason := r.FormValue("reason")

	if postID == "" {
		http.Error(w, "ID du post manquant", http.StatusBadRequest)
		return
	}
	
	query := `INSERT INTO Reports (id_publication, id_utilisateur, reason, commentaire) VALUES ($1, $2, $3, $4)`
	_, err := db.Exec(query, postID, userID, reason, comment)
	if err != nil {
		http.Error(w, "Erreur lors du signalement du post", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintln(w, "Post signalé avec succès")
}


// Handlers pour les likes et commentaires


func LikeHandler(w http.ResponseWriter, r *http.Request) {
	userID := -1
	if token := r.Header.Get("Authorization"); token != "" {
		userID = getUserIDFromToken(token)
	}


	post_id := r.URL.Query().Get("id")
	if post_id == "" {
		http.Error(w, "ID du post manquant", http.StatusBadRequest)
		return
	}

	query := `INSERT INTO Likes (id_user, id_pub) SELECT $1, $2 WHERE NOT EXISTS (SELECT 1 FROM Likes WHERE id_user = $1 AND id_pub = $2)`
	_, err := db.Exec(query, userID, post_id)
	if err != nil {
		http.Error(w, "Erreur lors de l'ajout du like", http.StatusInternalServerError)
		return
	}

	capturedPostID := post_id
	capturedLikerID := userID
	go func() {
		var authorID int
		var authorFCM sql.NullString
		var postDesc string
		err := db.QueryRow(
			`SELECT p.id_publicateur, u.fcm_token, p.description
			 FROM Publications p
			 JOIN Utilisateurs u ON u.usr_id = p.id_publicateur
			 WHERE p.id_pub = $1`, capturedPostID,
		).Scan(&authorID, &authorFCM, &postDesc)
		if err != nil || !authorFCM.Valid || authorFCM.String == "" || authorID == capturedLikerID {
			return
		}
		var likerUsername string
		if err = db.QueryRow(`SELECT username FROM Utilisateurs WHERE usr_id = $1`, capturedLikerID).Scan(&likerUsername); err != nil {
			return
		}
		NotifyLikePush(authorFCM.String, likerUsername, parseCaption(postDesc))
	}()

	w.WriteHeader(http.StatusOK)
	fmt.Fprintln(w, "Like ajouté avec succès")
}

func UnlikeHandler(w http.ResponseWriter, r *http.Request) {
	userID := -1
	if token := r.Header.Get("Authorization"); token != "" {
		userID = getUserIDFromToken(token)
	}
	post_id := r.URL.Query().Get("id")
	if post_id == "" {
		http.Error(w, "ID du post manquant", http.StatusBadRequest)
		return
	}

	query := `DELETE FROM Likes WHERE id_user = $1 AND id_pub = $2`
	_, err := db.Exec(query, userID, post_id)
	if err != nil {
		http.Error(w, "Erreur lors de la suppression du like", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintln(w, "Like supprimé avec succès")
}

func GetLikesHandler(w http.ResponseWriter, r *http.Request) {
	post_id := r.URL.Query().Get("id")
	if post_id == "" {
		http.Error(w, "ID du post manquant", http.StatusBadRequest)
		return
	}

	var count int
	query := `SELECT COUNT(*) FROM Likes WHERE id_pub = $1`
	err := db.Get(&count, query, post_id)
	if err != nil {
		http.Error(w, "Erreur lors de la récupération du nombre de likes", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]int{"likes_count": count})
}

func GetAllUserLikesHandler(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)

	var likedPosts []int
	query := `SELECT id_pub FROM Likes WHERE id_user = $1`
	err := db.Select(&likedPosts, query, userID)
	if err != nil {
		http.Error(w, "Erreur lors de la récupération des posts likés", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(likedPosts)
}


func CommentHandler(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(int)
	post_id := r.URL.Query().Get("id")

	if post_id == "" {
		http.Error(w, "ID du post manquant", http.StatusBadRequest)
		return
	}

	// 1. Parse le formulaire multipart (limite de 10 MB, largement suffisant pour un vocal)
	err := r.ParseMultipartForm(10 << 20)
	if err != nil {
		http.Error(w, "Fichiers trop volumineux", http.StatusBadRequest)
		return
	}

	comment := r.FormValue("comment")

	// --- GESTION DE L'AUDIO (OPTIONNEL) ---
	var audioURL sql.NullString

	audioFile, audioHandler, errAudio := r.FormFile("audio")
	if errAudio == nil {
		// Si un fichier audio est présent
		defer audioFile.Close()

		audioName := lib.GenerateNewUUID() + filepath.Ext(audioHandler.Filename)
		audioDstPath := "./uploads/comments_audio/" + audioName
		audioDst, err := os.Create(audioDstPath)
		if err != nil {
			http.Error(w, "Erreur lors de la création du fichier audio", http.StatusInternalServerError)
			return
		}
		defer audioDst.Close()

		// On copie directement le fichier audio
		if _, err := io.Copy(audioDst, audioFile); err != nil {
			http.Error(w, "Erreur lors de l'écriture de l'audio", http.StatusInternalServerError)
			return
		}

		// On valide la variable sql.NullString
		audioURL = sql.NullString{
			String: fmt.Sprintf("%s/uploads/comments_audio/%s", BaseURL, audioName),
			Valid:  true,
		}
	} else if errAudio != http.ErrMissingFile {
		// S'il y a une erreur autre que "fichier manquant"
		http.Error(w, "Erreur lors de la lecture du fichier audio", http.StatusBadRequest)
		return
	}

	// --- SAUVEGARDE EN BASE DE DONNÉES ---
	// Ajout de url_audio dans la requête SQL
	query := `INSERT INTO Commentaires (id_user, id_pub, commentaire, url_audio) VALUES ($1, $2, $3, $4)`
	_, err = db.Exec(query, userID, post_id, comment, audioURL)
	if err != nil {
		http.Error(w, "Erreur lors de l'ajout du commentaire", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintln(w, "Commentaire ajouté avec succès")
}

func GetCommentsHandler(w http.ResponseWriter, r *http.Request) {
	post_id := r.URL.Query().Get("id")
	if post_id == "" {
		http.Error(w, "ID du post manquant", http.StatusBadRequest)
		return
	}

	type Comment struct {
		ID          int     `db:"id_com" json:"id"`
		UserID      int     `db:"id_user" json:"user_id"` 
		PubID       int     `db:"id_pub" json:"post_id"` 
		Commentaire string  `db:"commentaire" json:"commentaire"`
		AudioURL    *string `db:"url_audio" json:"audio_url,omitempty"` // <--- Nouveau champ
	}

	var comments []Comment
	// Ajout de url_audio dans le SELECT
	query := `SELECT id_com, id_pub, id_user, commentaire, url_audio FROM Commentaires WHERE id_pub = $1`
	err := db.Select(&comments, query, post_id)
	if err != nil {
		http.Error(w, "Erreur lors de la récupération des commentaires", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(comments)
}



// algo

func GetNearbyPostsHandler(w http.ResponseWriter, r *http.Request) {
	gps := r.URL.Query().Get("gps")
	if gps == "" {
		http.Error(w, "Coordonnées GPS manquantes", http.StatusBadRequest)
		return
	}

	postIDs, err := lib.GetNearbyPostIDs(db, gps) 
	if err != nil {
		http.Error(w, "Erreur lors du calcul de proximité", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(postIDs) // Renvoie direct [1, 2, 3, 4, 5]
}


func GetPostPerUserHandler(w http.ResponseWriter, r *http.Request) {

	askerID := -1
	tokenString := r.Header.Get("Authorization")
	if tokenString != "" {
    	askerID = getUserIDFromToken(tokenString)
	}

	userID := r.URL.Query().Get("user_id")
	if userID == "" {
		return 
	}

	var posts []lib.PostResponse
	query := `SELECT id_pub, id_publicateur, description, url_image, url_audio, tags, date, id_localisation 
			  FROM Publications WHERE id_publicateur = $1` 
	err := db.Select(&posts, query, userID)
	if err != nil {
		return 
	}

	// check for posts that are in groups the user is part of

	for i := range posts {
		var groupIDs []int
		queryGroups := `SELECT id_grp FROM PublicationGroupe WHERE id_pub = $1`
		err = db.Select(&groupIDs, queryGroups, posts[i].ID)
		if err != nil {
			fmt.Printf("Erreur récupération groupes pour post %d: %v\n", posts[i].ID, err)
			continue
		}

		posts[i].Groupes = groupIDs

		if len(groupIDs) > 0 {
			authorized := false
			for _, gID := range groupIDs {
				if IsMemberOfGroup(askerID, gID) {
					authorized = true
					break
				}
			}
			if !authorized {
				posts[i] = lib.PostResponse{} 
			}
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(posts)
}