package handlers

import (
	"github.com/jmoiron/sqlx"
)

// Package-level variables that are initialized by main.go
var (
	db        *sqlx.DB
	jwtSecret []byte
	BaseURL   string
)

// InitHandlers initializes the handlers package with required dependencies
func InitHandlers(database *sqlx.DB, secret []byte, baseURL string) {
	db = database
	jwtSecret = secret
	BaseURL = baseURL
}
