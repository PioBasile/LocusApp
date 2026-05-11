package handlers

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"time"
)

// ─────────────────────────────────────────────────────────────────────────────
// GET /weather?lat=43.6&lon=3.87
// Proxy vers OpenWeatherMap. Retourne un résumé condensé pour que le moteur
// de génération d'itinéraires puisse adapter ses choix à la météo réelle.
//
// Variable d'environnement requise : OPENWEATHER_API_KEY
//   (sans clé → 503 Service Unavailable + payload de fallback "unknown")
// ─────────────────────────────────────────────────────────────────────────────

type weatherResponse struct {
	Lat            float64 `json:"lat"`
	Lon            float64 `json:"lon"`
	TempC          float64 `json:"temp_c"`
	FeelsLikeC     float64 `json:"feels_like_c"`
	Humidity       int     `json:"humidity"`
	WindSpeedMS    float64 `json:"wind_speed_ms"`
	CloudsPct      int     `json:"clouds_pct"`
	Condition      string  `json:"condition"`      // "Clear", "Rain", "Snow", "Clouds"...
	Description    string  `json:"description"`    // "ciel dégagé", "pluie modérée"...
	IconCode       string  `json:"icon_code"`
	IsRain         bool    `json:"is_rain"`
	IsSnow         bool    `json:"is_snow"`
	IsGoodOutdoor  bool    `json:"is_good_for_outdoor"`
	FetchedAt      string  `json:"fetched_at"`
}

// payload natif OpenWeatherMap (extrait des champs qu'on utilise)
type owmPayload struct {
	Weather []struct {
		Main        string `json:"main"`
		Description string `json:"description"`
		Icon        string `json:"icon"`
	} `json:"weather"`
	Main struct {
		Temp      float64 `json:"temp"`
		FeelsLike float64 `json:"feels_like"`
		Humidity  int     `json:"humidity"`
	} `json:"main"`
	Wind struct {
		Speed float64 `json:"speed"`
	} `json:"wind"`
	Clouds struct {
		All int `json:"all"`
	} `json:"clouds"`
	Cod interface{} `json:"cod"` // peut être int ou string en cas d'erreur
}

var weatherHTTPClient = &http.Client{Timeout: 5 * time.Second}

func WeatherHandler(w http.ResponseWriter, r *http.Request) {
	latStr := r.URL.Query().Get("lat")
	lonStr := r.URL.Query().Get("lon")
	if latStr == "" || lonStr == "" {
		http.Error(w, "Paramètres lat et lon requis", http.StatusBadRequest)
		return
	}
	lat, err1 := strconv.ParseFloat(latStr, 64)
	lon, err2 := strconv.ParseFloat(lonStr, 64)
	if err1 != nil || err2 != nil {
		http.Error(w, "lat/lon invalides", http.StatusBadRequest)
		return
	}

	apiKey := os.Getenv("OPENWEATHER_API_KEY")
	if apiKey == "" {
		// Fallback minimal pour ne pas casser l'algo en l'absence de clé
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusServiceUnavailable)
		json.NewEncoder(w).Encode(map[string]interface{}{
			"error":         "OPENWEATHER_API_KEY non configurée",
			"is_good_for_outdoor": true, // fallback neutre
			"condition":     "unknown",
		})
		return
	}

	owmURL := fmt.Sprintf(
		"https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&units=metric&lang=fr&appid=%s",
		url.QueryEscape(latStr), url.QueryEscape(lonStr), url.QueryEscape(apiKey),
	)

	resp, err := weatherHTTPClient.Get(owmURL)
	if err != nil {
		http.Error(w, "Erreur de connexion au service météo", http.StatusBadGateway)
		return
	}
	defer resp.Body.Close()

	body, _ := io.ReadAll(resp.Body)
	if resp.StatusCode != http.StatusOK {
		http.Error(w, fmt.Sprintf("Service météo : statut %d", resp.StatusCode), http.StatusBadGateway)
		return
	}

	var payload owmPayload
	if err := json.Unmarshal(body, &payload); err != nil {
		http.Error(w, "Réponse météo illisible", http.StatusBadGateway)
		return
	}

	out := weatherResponse{
		Lat:         lat,
		Lon:         lon,
		TempC:       payload.Main.Temp,
		FeelsLikeC:  payload.Main.FeelsLike,
		Humidity:    payload.Main.Humidity,
		WindSpeedMS: payload.Wind.Speed,
		CloudsPct:   payload.Clouds.All,
		FetchedAt:   time.Now().UTC().Format(time.RFC3339),
	}
	if len(payload.Weather) > 0 {
		out.Condition = payload.Weather[0].Main
		out.Description = payload.Weather[0].Description
		out.IconCode = payload.Weather[0].Icon
	}
	out.IsRain = out.Condition == "Rain" || out.Condition == "Drizzle" || out.Condition == "Thunderstorm"
	out.IsSnow = out.Condition == "Snow"
	// Heuristique "bon pour activités extérieures" : pas de pluie/neige, vent < 10 m/s, temp ≥ 10 °C
	out.IsGoodOutdoor = !out.IsRain && !out.IsSnow && out.WindSpeedMS < 10 && out.TempC >= 10

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(out)
}