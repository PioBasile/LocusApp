package main

import (
	"fmt"
	"log"
	"net/http"
)

// TEMPORAIRE

var usersDB = []User{}

// ----

func main() {
	http.HandleFunc("/login", LoginHandler)
	http.HandleFunc("/profile", IsAuthorized(GetProfileHandler))
	http.HandleFunc("/signup", SignupHandler)

	fmt.Println("Serveur démarré sur http://localhost:8080")
	
	err := http.ListenAndServe(":8080", nil)
	if err != nil {
		log.Fatal(err)
	}
}


