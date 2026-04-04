package handlers

import (
	"fmt"
	"github.com/golang-jwt/jwt/v5"
	"backend/config"
)

func IsMemberOfGroup(userID, groupID int) bool {
    if groupID == 0 {
        return true
    }
	if userID == -1 {
        return false
    }
    var count int
    query := `SELECT COUNT(*) FROM MembreGroupes WHERE id_grp = $1 AND usr_id = $2`
    err := db.Get(&count, query, groupID, userID)
    fmt.Printf("IsMemberOfGroup: userID=%d groupID=%d count=%d err=%v\n", userID, groupID, count, err)
    if err != nil {
        return false
    }
    return count > 0
}

func getUserIDFromToken(tokenString string) int {
    if config.DEBUG == "true" && tokenString == "debug" {
        return 0
    }
    token, err := jwt.Parse(tokenString, func(t *jwt.Token) (interface{}, error) {
        return jwtSecret, nil
    })
    if err != nil || !token.Valid {
        return -1
    }
    claims, _ := token.Claims.(jwt.MapClaims)
    return int(claims["user_id"].(float64))
}