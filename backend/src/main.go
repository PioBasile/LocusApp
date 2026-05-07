package main

import (
	"fmt"
	"log"
	"os"

	"backend/config"
	"backend/db"
	"backend/server"
)

func main() {

	initDirectories()

	// Initialize configuration
	if err := config.Init(); err != nil {
		log.Fatal("Failed to initialize config:", err)
	}

	// Initialize database
	log.Printf("Connecting to database: %s at %s:%s...", config.DBName, config.DBHost, config.DBPort)
	database, err := db.Init()
	if err != nil {
		log.Fatalf("Failed to initialize database: %v", err)
	}
	defer db.Close()
	log.Println("✓ Database connected successfully")

	// Print startup message
	fmt.Printf("Serveur démarré sur %s\n", config.BaseURL)

	// Start server
	log.Printf("Server listening on %s:%s", config.ServerHost, config.ServerPort)
	if err := server.Start(database); err != nil {
		log.Fatalf("Server error: %v", err)
	}
}


func initDirectories() {
    // Liste des dossiers requis pour les uploads
    dirs := []string{
        "./uploads/posts",
        "./uploads/groupes_pic",
        "./uploads/profiles_pic",
    }

    for _, dir := range dirs {
        // MkdirAll ne fait rien si le dossier existe déjà
        err := os.MkdirAll(dir, 0755)
        if err != nil {
            log.Fatalf("Erreur critique : impossible de créer le dossier %s : %v", dir, err)
        }
    }
}