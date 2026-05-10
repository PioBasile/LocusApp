#!/bin/bash

# Configuration
BASE_URL="http://localhost:8080"
TOKEN="debug" # Utilise "debug" si config.DEBUG="true", sinon mets ton JWT ici
CONTENT_TYPE="Content-Type: application/json"
AUTH="Authorization: $TOKEN"

echo "--- 1. Création d'un lieu (Restaurant) ---"
LIEU_ID=$(curl -s -X POST "$BASE_URL/travelPath/lieux/create" \
  -H "$CONTENT_TYPE" \
  -H "$AUTH" \
  -d '{
    "nom": "Le Petit Sommelier",
    "description": "Un restaurant gastronomique",
    "adresse": "12 rue de la Loge, Montpellier",
    "categorie": "restaurant",
    "lat": 43.6107,
    "lon": 3.8767,
    "prix_moyen": 45
  }' | jq -r '.id_lieu')
echo "Lieu créé avec l'ID: $LIEU_ID"
echo ""

echo "--- 2. Sauvegarde d'un itinéraire ---"
ITIN_ID=$(curl -s -X POST "$BASE_URL/travelPath/itineraires/save" \
  -H "$CONTENT_TYPE" \
  -H "$AUTH" \
  -d '{
    "nom": "Ma balade gourmande",
    "type": "equilibre",
    "budget_total": 60,
    "duree_minutes": 120,
    "effort_score": 2,
    "etapes": [
      {
        "ordre": 1,
        "id_lieu": '$LIEU_ID',
        "nom_lieu": "Le Petit Sommelier",
        "categorie": "restaurant",
        "prix": 45
      }
    ]
  }' | jq -r '.id')
echo "Itinéraire sauvegardé avec l'ID: $ITIN_ID"
echo ""

echo "--- 3. Récupération de mes itinéraires ---"
curl -s -X GET "$BASE_URL/travelPath/itineraires" \
  -H "$AUTH" | jq '.'
echo ""

echo "--- 4. Liker l'itinéraire ---"
curl -s -X POST "$BASE_URL/travelPath/itineraires/like?id=$ITIN_ID" \
  -H "$AUTH" | jq '.'
echo ""

echo "--- 5. Recherche par catégorie (restaurant) ---"
curl -s -X GET "$BASE_URL/travelPath/itineraires/search?categories=restaurant" \
  -H "$AUTH" | jq '.'
echo ""

echo "--- 6. Recherche textuelle (gourmande) ---"
curl -s -X GET "$BASE_URL/travelPath/itineraires/search?q=gourmande" \
  -H "$AUTH" | jq '.'
echo ""

echo "--- 7. Unlike l'itinéraire ---"
curl -s -X DELETE "$BASE_URL/travelPath/itineraires/unlike?id=$ITIN_ID" \
  -H "$AUTH"
echo "Itinéraire unliké."
echo ""