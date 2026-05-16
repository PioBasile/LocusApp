package config

import "os"

// Database configuration
var (
	DBUser     = getEnv("DB_USER", "user_go")
	DBPassword = getEnv("DB_PASSWORD", "password_go")
	DBName     = getEnv("DB_NAME", "travelshare_db")
	DBHost     = getEnv("DB_HOST", "localhost")
	DBPort     = getEnv("DB_PORT", "5432")
)

// A changer pour déployer
var DEBUG = "true"

// Server configuration
const (
	ServerHost = "0.0.0.0"
	ServerPort = "8080"
	BaseURL    = "https://mobile.piorian.fr"
)

// File uploads configuration
const (
	UploadDir      = "./uploads"
	PostsUploadDir = "./uploads/posts"
)

// getEnv gets environment variable with fallback default
func getEnv(key, defaultVal string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultVal
}

// GetDSN builds the database connection string
func GetDSN() string {
	return "user=" + DBUser + " password=" + DBPassword + " dbname=" + DBName + " host=" + DBHost + " port=" + DBPort + " sslmode=disable"
}

// GetServerAddr returns the full server address
func GetServerAddr() string {
	return ServerHost + ":" + ServerPort
}

// Init creates necessary directories
func Init() error {
	return os.MkdirAll(PostsUploadDir, os.ModePerm)
}
