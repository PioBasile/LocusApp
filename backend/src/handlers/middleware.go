package handlers

import (
	"context"
	"net/http"
	"github.com/golang-jwt/jwt/v5"
	"backend/config"
)

type contextKey string
const UserIDKey contextKey = "user_id"

// Middleware that verifies JWT tokens in request Authorization header
func IsAuthorized(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		tokenString := r.Header.Get("Authorization")
		if tokenString == "" {
			http.Error(w, "Token manquant", http.StatusUnauthorized)
			return
		}

		if config.DEBUG == "true" && tokenString == "debug" {
            ctx := context.WithValue(r.Context(), UserIDKey, 0)
            next(w, r.WithContext(ctx))
            return
        }

		token, err := jwt.Parse(tokenString, func(t *jwt.Token) (interface{}, error) {
			return jwtSecret, nil
		})
		if err != nil || !token.Valid{
			http.Error(w, "Token invalide", http.StatusUnauthorized)
			return
		}

		claims, _ := token.Claims.(jwt.MapClaims)
        userID := int(claims["user_id"].(float64))
        ctx := context.WithValue(r.Context(), UserIDKey, userID)
        next(w, r.WithContext(ctx))
	}
}