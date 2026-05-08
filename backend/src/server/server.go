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
	http.HandleFunc("/getGroupInfo", handlers.GetGroupByIDHandler)
	http.HandleFunc("/like", handlers.LikeHandler)
	http.HandleFunc("/getNearbyPosts", handlers.GetNearbyPostsHandler)
	http.HandleFunc("/getComments", handlers.GetCommentsHandler)
	http.HandleFunc("/getLikes", handlers.GetLikesHandler)
	http.HandleFunc("/getMostFollowedUsers", handlers.GetTopMostFollowedUsers)
	http.HandleFunc("/getAllUserPosts", handlers.GetPostPerUserHandler)

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
	http.HandleFunc("/getUserGroups", handlers.IsAuthorized(handlers.GetUserGroupsHandler))
	http.HandleFunc("/unlike", handlers.IsAuthorized(handlers.UnlikeHandler))
	http.HandleFunc("/getAllUserLikes", handlers.IsAuthorized(handlers.GetAllUserLikesHandler))
	http.HandleFunc("/reportPost", handlers.IsAuthorized(handlers.ReportPostHandler))
	http.HandleFunc("/comment", handlers.IsAuthorized(handlers.CommentHandler))
	http.HandleFunc("/deletePost", handlers.IsAuthorized(handlers.DeletePostHandler))
	http.HandleFunc("/updateFCMToken", handlers.IsAuthorized(handlers.UpdateFCMTokenHandler))
	http.HandleFunc("/ChangeUsername", handlers.IsAuthorized(handlers.ChangeUsernameHandler))
	
	
	
}

func setupFileServer() {
	fs := http.FileServer(http.Dir(config.UploadDir))
	http.Handle("/uploads/", http.StripPrefix("/uploads/", fs))
}

func Start(database *sqlx.DB) error {
	handlers.InitHandlers(database, lib.JWTSecret, config.BaseURL)

	setupRoutes()
	setupFileServer()

	return http.ListenAndServe(config.GetServerAddr(), nil)
}
