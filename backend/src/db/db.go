package db

import (
	"backend/config"
	"github.com/jmoiron/sqlx"
	_ "github.com/lib/pq"
)

var instance *sqlx.DB

// Init initializes the database connection
func Init() (*sqlx.DB, error) {
	var err error
	instance, err = sqlx.Connect("postgres", config.GetDSN())
	if err != nil {
		return nil, err
	}
	return instance, nil
}

// Get returns the database instance
func Get() *sqlx.DB {
	return instance
}

// Close closes the database connection
func Close() error {
	if instance != nil {
		return instance.Close()
	}
	return nil
}
