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


type PostResponse struct {
    ID          int    `json:"id"`
    Title       string `json:"title"`
    Description string `json:"description"`
    ImageURL    string `json:"image_url"`
    Message     string `json:"message"`
}

type Location struct {
	ID  int     `json:"id" db:"id_loc"`
	Nom string  `json:"nom" db:"nom"`
	GPS [2]float64 `json:"gps" db:"gps"`
}

type PublicUserInfo struct {
	ID             int    `json:"id" db:"id"`
	Username       string `json:"username" db:"username"`
	ProfilePicture string `json:"ppurl" db:"ppurl"`
}