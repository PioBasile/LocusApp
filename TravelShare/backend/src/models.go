package main

type User struct {
    ID             int    `json:"id" db:"id"`
    Password       string `json:"-" db:"password"`
    Email          string `json:"email" db:"email"`
    Username       string `json:"username" db:"username"`
    ProfilePicture string `json:"ppurl" db:"ppurl"`
}

type LoginRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}
	
type LoginResponse struct {
	Token string `json:"token"`
}