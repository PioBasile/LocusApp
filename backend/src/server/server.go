package server

import (
	"net/http"

	"backend/config"
	"backend/handlers"
	"backend/lib"
	"github.com/jmoiron/sqlx"
)

// setupRoutes registers all HTTP handlers
func setupRoutes() {
	// Public routes
	http.HandleFunc("/login", handlers.LoginHandler)
	http.HandleFunc("/signup", handlers.SignupHandler)
	http.HandleFunc("/getpost", handlers.GetPostHandler)
	http.HandleFunc("/getPublicProfile", handlers.GetPublicProfileHandler)
	http.HandleFunc("/getLocations", handlers.GetLocalisationHandler)
	http.HandleFunc("/getGroups", handlers.GetGroupsHandler)

	// Protected routes
	http.HandleFunc("/makepost", handlers.IsAuthorized(handlers.MakePostHandler))
	http.HandleFunc("/profile", handlers.IsAuthorized(handlers.GetProfileHandler))
	http.HandleFunc("/getPostsByGroup", handlers.IsAuthorized(handlers.GetPostPerGroupHandler))
	http.HandleFunc("/makeGroup", handlers.IsAuthorized(handlers.MakeGroupHandler))
	http.HandleFunc("/joinGroup", handlers.IsAuthorized(handlers.JoinGroupHandler))
	http.HandleFunc("/follow", handlers.IsAuthorized(handlers.Follow))
	http.HandleFunc("/unfollow", handlers.IsAuthorized(handlers.Unfollow))
	http.HandleFunc("/getFollowers", handlers.IsAuthorized(handlers.GetFollowers))
	http.HandleFunc("/changePP", handlers.IsAuthorized(handlers.ChangePPHandler))
	
}

// setupFileServer configures static file serving for uploads
func setupFileServer() {
	fs := http.FileServer(http.Dir(config.UploadDir))
	http.Handle("/uploads/", http.StripPrefix("/uploads/", fs))
}

// Start initializes and starts the HTTP server
func Start(database *sqlx.DB) error {
	// Initialize handlers with dependencies
	handlers.InitHandlers(database, lib.JWTSecret, config.BaseURL)

	setupRoutes()
	setupFileServer()

	return http.ListenAndServe(config.GetServerAddr(), nil)
}
