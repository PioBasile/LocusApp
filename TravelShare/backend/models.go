package main

type User struct {
	ID    int    `json:"id"`
	Password   string `json:"-"`
	Email string `json:"email"`
	Username string `json:"username"`
	ProfilePicture string `json:"ppurl"`
}

type LoginRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}
	
type LoginResponse struct {
	Token string `json:"token"`
}