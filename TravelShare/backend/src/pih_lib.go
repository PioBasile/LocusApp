package main

import (
	"time"
	"golang.org/x/crypto/bcrypt"
	"github.com/golang-jwt/jwt/v5"
)

var jwtSecret = []byte("test1234")

func HashPassword(password string) (string, error) {
    bytes, err := bcrypt.GenerateFromPassword([]byte(password), 14)
    return string(bytes), err
}


func GenerateJWT(user User) (string, error) {

    claims := jwt.MapClaims{
        "user_id":  user.ID,
        "username": user.Username,
        "exp":      time.Now().Add(time.Hour * 24).Unix(), 
    }

    token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)

    return token.SignedString(jwtSecret)
}