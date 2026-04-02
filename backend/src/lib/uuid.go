package lib

import (
	"github.com/google/uuid"
)

// GenerateNewUUID generates a new UUID v4 string
func GenerateNewUUID() string {
	return uuid.New().String()
}
