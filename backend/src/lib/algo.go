package lib

import (
	"fmt"
	"math"
	"sort"
	"strconv"
	"strings"
	"github.com/jmoiron/sqlx"
)

// Calcule la distance de Haversine entre deux points "lat,lon"
func CalculateDistance(gps1, gps2 string) float64 {
	p1Lat, p1Lon, err1 := parseGPS(gps1)
	p2Lat, p2Lon, err2 := parseGPS(gps2)
	if err1 != nil || err2 != nil {
		return math.MaxFloat64
	}

	const R = 6371.0 // Rayon de la Terre en km
	phi1 := p1Lat * math.Pi / 180
	phi2 := p2Lat * math.Pi / 180
	deltaPhi := (p2Lat - p1Lat) * math.Pi / 180
	deltaLambda := (p2Lon - p1Lon) * math.Pi / 180

	a := math.Sin(deltaPhi/2)*math.Sin(deltaPhi/2) +
		math.Cos(phi1)*math.Cos(phi2)*
			math.Sin(deltaLambda/2)*math.Sin(deltaLambda/2)
	c := 2 * math.Atan2(math.Sqrt(a), math.Sqrt(1-a))

	return R * c
}

func parseGPS(gps string) (lat, lon float64, err error) {
	parts := strings.Split(gps, ",")
	if len(parts) != 2 {
		return 0, 0, fmt.Errorf("format invalide")
	}
	lat, _ = strconv.ParseFloat(strings.TrimSpace(parts[0]), 64)
	lon, _ = strconv.ParseFloat(strings.TrimSpace(parts[1]), 64)
	return lat, lon, nil
}

// Récupère les 5 IDs les plus proches
func GetNearbyPostIDs(db *sqlx.DB, userGPS string) ([]int, error) {
	var allPosts []Post_for_algo
	// Note: il faut que ta table Localisations ou Publications ait une colonne GPS
	// Ici je suppose que tu joins la table Localisations pour avoir les coordonnées
	query := `SELECT p.id_pub, l.gps 
	          FROM Publications p 
	          JOIN Localisations l ON p.id_localisation = l.id_loc`
	
	err := db.Select(&allPosts, query)
	if err != nil {
		return nil, err
	}

	// Calcul des distances
	for i := range allPosts {
		allPosts[i].DistanceFromUser = CalculateDistance(userGPS, allPosts[i].GPS)
	}

	// Tri par distance croissante
	sort.Slice(allPosts, func(i, j int) bool {
		return allPosts[i].DistanceFromUser < allPosts[j].DistanceFromUser
	})

	// Récupération des 5 premiers IDs
	limit := 5
	if len(allPosts) < 5 {
		limit = len(allPosts)
	}

	result := make([]int, 0, limit)
	for i := 0; i < limit; i++ {
		result = append(result, allPosts[i].ID)
	}

	return result, nil
}