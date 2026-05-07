package lib

// User represents a user in the system
type User struct {
	ID             int    `json:"id" db:"id"`
	Password       string `json:"-" db:"password"`
	Email          string `json:"email" db:"email"`
	Username       string `json:"username" db:"username"`
	ProfilePicture string `json:"ppurl" db:"ppurl"`
	FCMToken       string `json:"fcm_token" db:"fcm_token"`
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
    ID          int            `db:"id_pub" json:"id"`
    UserID      int            `db:"id_publicateur" json:"user_id"`
    Groupes      []int            `db:"groupe" json:"groupe"`
    Description string         `db:"description" json:"description"`
    ImageURL    string         `db:"url_image" json:"image_url"`
    Date        string         `db:"date" json:"date"`
    LocID       *int           `db:"id_localisation" json:"id_loc"`
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

// Group represents a user group
type Group struct {
	ID          int    `json:"id" db:"id_groupe"`
	Name        string `json:"name" db:"nom"`
	Description string `json:"description" db:"description"`
	imageURL    string `json:"image_url" db:"url_image"`
}


type Post_for_algo struct {
    ID      int     `db:"id_pub"`   
    GPS     string  `db:"gps"`     
	DistanceFromUser float64 
}