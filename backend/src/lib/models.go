package lib

import "github.com/lib/pq"
import "time"
import "encoding/json"

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
	Groupes     []int          `db:"groupe" json:"groupe"`
	Description string         `db:"description" json:"description"`
	ImageURL    string         `db:"url_image" json:"image_url"`
	AudioURL    *string        `db:"url_audio" json:"audio_url,omitempty"`
	Tags        pq.StringArray `db:"tags" json:"tags"`
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
	Posts 		   []PostResponse `json:"posts,omitempty"`
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

type FollowerInfo struct {
	ID       int    `json:"id" db:"id"`
	Username string `json:"username" db:"username"`
	PPURL    string `json:"ppurl" db:"ppurl"`
}




type Lieu struct {
	ID          int     `db:"id_lieu" json:"id"`
	Nom         string  `db:"nom" json:"nom"`
	Description string  `db:"description" json:"description"`
	Adresse     string  `db:"adresse" json:"adresse"`
	Categorie   string  `db:"categorie" json:"categorie"`
	GPS         string  `db:"gps" json:"gps"`
	URLImage    *string `db:"url_image" json:"url_image,omitempty"`
	Note        float64 `db:"note" json:"note"`
	NbAvis      int     `db:"nb_avis" json:"nb_avis"`
	Horaires    *string `db:"horaires" json:"horaires,omitempty"`
	PrixMoyen   int     `db:"prix_moyen" json:"prix_moyen"`
	SiteWeb     *string `db:"site_web" json:"site_web,omitempty"`
	Telephone   *string `db:"telephone" json:"telephone,omitempty"`
	IDLoc       *int    `db:"id_loc" json:"id_loc,omitempty"`
	// Champs calculés (non stockés en DB)
	Distance  float64       `db:"-" json:"distance_km,omitempty"`
	Photos    []LieuPhoto   `db:"-" json:"photos,omitempty"`
}

// LieuPhoto représente une photo additionnelle d'un lieu
type LieuPhoto struct {
	ID      int    `db:"id_photo" json:"id"`
	IDLieu  int    `db:"id_lieu" json:"id_lieu"`
	URL     string `db:"url" json:"url"`
	Legende string `db:"legende" json:"legende"`
	Ordre   int    `db:"ordre" json:"ordre"`
}

// LieuAvis représente un avis utilisateur sur un lieu
type LieuAvis struct {
	ID          int       `db:"id_avis" json:"id"`
	IDLieu      int       `db:"id_lieu" json:"id_lieu"`
	UsrID       int       `db:"usr_id" json:"usr_id"`
	Username    string    `db:"-" json:"username"`
	Note        int       `db:"note" json:"note"`
	Commentaire string    `db:"commentaire" json:"commentaire"`
	CreatedAt   time.Time `db:"created_at" json:"created_at"`
}

// ItineraireEtape représente une étape dans un itinéraire
type ItineraireEtape struct {
	Ordre           int     `json:"ordre"`
	IDLieu          int     `json:"id_lieu"`
	NomLieu         string  `json:"nom_lieu"`
	AdresseLieu     string  `json:"adresse_lieu"`
	GPSLieu         string  `json:"gps_lieu"`
	Categorie       string  `json:"categorie"`
	Creneau         string  `json:"creneau"`           // "matin", "apres-midi", "soir"
	DureeMinutes    int     `json:"duree_minutes"`
	DistancePrevKm  float64 `json:"distance_prev_km"`  // distance depuis étape précédente
	TempsTrajMin    int     `json:"temps_trajet_min"`  // temps de trajet estimé
	Prix            int     `json:"prix"`
	Horaires        string  `json:"horaires"`
	URLImage        string  `json:"url_image"`
	Note            float64 `json:"note"`
}

// Itineraire représente un itinéraire complet généré
type Itineraire struct {
	ID           int               `json:"id"`
	Type         string            `json:"type"`             // "economique", "equilibre", "confort"
	Nom          string            `json:"nom"`
	Etapes       []ItineraireEtape `json:"etapes"`
	BudgetTotal  int               `json:"budget_total"`
	DureeMinutes int               `json:"duree_minutes"`
	EffortScore  int               `json:"effort_score"`     // 1-5
	MeteoSensible bool             `json:"meteo_sensible"`
	Resume       string            `json:"resume"`
}

// ItineraireRequest représente une requête de génération d'itinéraire
type ItineraireRequest struct {
	GPS           string   `json:"gps"`               // coordonnées GPS de l'utilisateur
	Categories    []string `json:"categories"`        // catégories souhaitées
	BudgetMax     int      `json:"budget_max"`
	DureeHeures   int      `json:"duree_heures"`
	Effort        string   `json:"effort"`            // "faible", "modere", "eleve"
	ToutTemps     bool     `json:"tout_temps"`
	LieuxFavoris  []string `json:"lieux_favoris"`
}

// LieuCreateRequest requête de création d'un lieu
type LieuCreateRequest struct {
	Nom         string  `json:"nom"`
	Description string  `json:"description"`
	Adresse     string  `json:"adresse"`
	Categorie   string  `json:"categorie"`
	Lat         float64 `json:"lat"`
	Lon         float64 `json:"lon"`
	Horaires    string  `json:"horaires"`
	PrixMoyen   int     `json:"prix_moyen"`
	SiteWeb     string  `json:"site_web"`
	Telephone   string  `json:"telephone"`
}

// SearchPostsRequest requête de recherche de posts
type SearchPostsRequest struct {
	Query      string   `json:"q"`
	GPS        string   `json:"gps"`
	RadiusKm   float64  `json:"radius_km"`
	Tags       []string `json:"tags"`
	Categories []string `json:"categories"`
	DateFrom   string   `json:"date_from"`
	DateTo     string   `json:"date_to"`
	Limit      int      `json:"limit"`
	Offset     int      `json:"offset"`
}

type ItineraireLike struct {
	ID           int `db:"id_like" json:"id"`
	UserID       int `db:"usr_id" json:"user_id"`
	ItineraireID int `db:"id_itin" json:"itin_id"`
}

// ItineraireSearchRequest pour filtrer par types de lieux
type ItineraireSearchRequest struct {
	Categories []string `json:"categories"` // ex: ["restaurant", "bar", "parc"]
	Query      string   `json:"q"`          // recherche textuelle
}

type SavedItin struct {
    ID        int             `json:"id"`
    Nom       string          `json:"nom"`
    Type      string          `json:"type"`
    Budget    int             `json:"budget"`
    DureeMin  int             `json:"duree_minutes"`
    Effort    int             `json:"effort_score"`
    Donnees   json.RawMessage `json:"itineraire"`
    CreatedAt time.Time       `json:"created_at"`
}