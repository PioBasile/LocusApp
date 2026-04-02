package lib

// User represents a user in the system
type User struct {
	ID             int    `json:"id" db:"id"`
	Password       string `json:"-" db:"password"`
	Email          string `json:"email" db:"email"`
	Username       string `json:"username" db:"username"`
	ProfilePicture string `json:"ppurl" db:"ppurl"`
}

// LoginRequest represents the login request payload
type LoginRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
	Username string `json:"username"`
}

// LoginResponse represents the login response with JWT token
type LoginResponse struct {
	Token string `json:"token"`
}

// PostResponse represents a post in responses
type PostResponse struct {
	ID          int    `json:"id"`
	Title       string `json:"title"`
	Description string `json:"description"`
	ImageURL    string `json:"image_url"`
	Message     string `json:"message"`
}

// Location represents a geographic location
type Location struct {
	ID  int         `json:"id" db:"id_loc"`
	Nom string      `json:"nom" db:"nom"`
	GPS [2]float64  `json:"gps" db:"gps"`
}

// PublicUserInfo represents public user profile information
type PublicUserInfo struct {
	ID             int    `json:"id" db:"id"`
	Username       string `json:"username" db:"username"`
	ProfilePicture string `json:"ppurl" db:"ppurl"`
}
