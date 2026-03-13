package main

import (
	"fmt"
	"log"
	"net/http"
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
	http.HandleFunc("/profile", IsAuthorized(GetProfileHandler))
	http.HandleFunc("/signup", SignupHandler)

	fmt.Println("Serveur démarré sur http://localhost:8080")
	
	err := http.ListenAndServe(":8080", nil)
	if err != nil {
		log.Fatal(err)
	}
}


