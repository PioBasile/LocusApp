package main

import (
	"fmt"
	"log"
	"net/http"
	"os"           


	"github.com/jmoiron/sqlx"
	_ "github.com/lib/pq"
)

// TEMPORAIRE

var db *sqlx.DB

func initDB() {
	var err error
	dsn := "user=user_go password=password_go dbname=travelshare_db host=db sslmode=disable"
	db, err = sqlx.Connect("postgres", dsn)
	if err != nil {
		log.Fatal(err)
	}
}

// ----

func main() {

	initDB()

	http.HandleFunc("/login", LoginHandler)
	http.HandleFunc("/signup", SignupHandler)
	http.HandleFunc("/getpost", GetPostHandler) 
	http.HandleFunc("/getPublicProfile", GetPublicProfileHandler)
	http.HandleFunc("/getLocations", GetLocalisationHandler)
	http.HandleFunc("/makepost", IsAuthorized(MakePostHandler))
	http.HandleFunc("/profile", IsAuthorized(GetProfileHandler))

	fmt.Println("Serveur démarré sur http://localhost:8080")

	fs := http.FileServer(http.Dir("./uploads"))
    http.Handle("/uploads/", http.StripPrefix("/uploads/", fs))
	os.MkdirAll("./uploads/posts", os.ModePerm)


	err := http.ListenAndServe(":8080", nil)
	if err != nil {
		log.Fatal(err)
	}
}
