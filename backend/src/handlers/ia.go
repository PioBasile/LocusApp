package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"strings"

	"github.com/google/generative-ai-go/genai"
	"github.com/lib/pq"
	"google.golang.org/api/option"
)

// GenerateAndSaveTags utilise Gemini pour analyser l'image et le texte
func GenerateAndSaveTags(postID int, description string, imageLocalPath string) {
	ctx := context.Background()
	
	// Récupère ta clé d'API (à définir dans ton terminal ou ton fichier .env)
	apiKey := os.Getenv("GEMINI_API_KEY")
	if apiKey == "" {
		log.Println("Erreur: GEMINI_API_KEY n'est pas définie")
		return
	}

	client, err := genai.NewClient(ctx, option.WithAPIKey(apiKey))
	if err != nil {
		log.Println("Erreur création client Gemini:", err)
		return
	}
	defer client.Close()

	// Gemini 1.5 Flash est le meilleur modèle pour des tâches rapides texte+image
	model := client.GenerativeModel("gemini-2.5-flash")
	
	// On force le modèle à nous renvoyer du JSON (très pratique pour parser)
	model.ResponseMIMEType = "application/json"

	// Le prompt (les instructions pour l'IA)
	promptText := fmt.Sprintf(`Analyse cette publication.
Description de l'utilisateur : "%s"
Génère un maximum de 5 tags pertinents (en un seul mot chacun, en minuscules, sans le symbole #).
Renvoie UNIQUEMENT un tableau JSON de strings. Exemple: ["nature", "voyage", "soleil"]`, description)

	var reqData []genai.Part
	reqData = append(reqData, genai.Text(promptText))

	// Si on a une image, on l'ajoute à la requête pour que l'IA la "voie"
	if imageLocalPath != "" {
		imgBytes, err := os.ReadFile(imageLocalPath)
		if err == nil {
			// Le SDK attend juste "jpeg" ou "png", sans le "image/" !
			format := "jpeg"
			if strings.HasSuffix(strings.ToLower(imageLocalPath), ".png") {
				format = "png"
			}
			reqData = append(reqData, genai.ImageData(format, imgBytes))
		}
	}

	// Appel à l'API Gemini
	resp, err := model.GenerateContent(ctx, reqData...)
	if err != nil {
		log.Println("Erreur appel Gemini:", err)
		return
	}

	// Récupération de la réponse
	if len(resp.Candidates) == 0 || len(resp.Candidates[0].Content.Parts) == 0 {
		return
	}

	part := resp.Candidates[0].Content.Parts[0]
	txt, ok := part.(genai.Text)
	if !ok {
		return
	}

	// Transformation du JSON en tableau de string Go
	var tags []string
	if err := json.Unmarshal([]byte(txt), &tags); err != nil {
		log.Printf("Erreur parsing JSON Gemini. Reçu: %s\n", txt)
		return
	}

	// Sauvegarde en base de données avec pq.Array pour PostgreSQL
	query := `UPDATE Publications SET tags = $1 WHERE id_pub = $2`
	_, err = db.Exec(query, pq.Array(tags), postID)
	if err != nil {
		log.Println("Erreur DB update tags:", err)
	} else {
		log.Printf("🤖 Tags générés pour le post %d : %v\n", postID, tags)
	}
}