#!/bin/bash

# Configuration
BASE_URL="http://localhost:8080" # Vérifiez votre config.BaseURL
IMAGE="/home/dorian/Téléchargements/ev.png"
EMAIL="test_complet@example.com"
PASSWORD="password123"
USERNAME="DorianTester"

echo "=== DÉMARRAGE DES TESTS DE L'API ==="

# 1. SIGNUP
echo -e "\n[1] Inscription..."
curl -s -X POST "$BASE_URL/signup" \
     -H "Content-Type: application/json" \
     -d "{\"email\":\"$EMAIL\", \"password\":\"$PASSWORD\", \"username\":\"$USERNAME\"}"

# 2. LOGIN
echo -e "\n[2] Connexion..."
LOGIN_RES=$(curl -s -X POST "$BASE_URL/login" \
     -H "Content-Type: application/json" \
     -d "{\"email\":\"$EMAIL\", \"password\":\"$PASSWORD\"}")
TOKEN=$(echo $LOGIN_RES | grep -oP '(?<="token":")[^"]*')

if [ -z "$TOKEN" ]; then echo "Échec de l'authentification"; exit 1; fi
echo "JWT obtenu."

# 3. PROFILE (Moi)
echo -e "\n[3] Récupération de mon profil..."
MY_ID=$(curl -s -X GET "$BASE_URL/profile" -H "Authorization: $TOKEN" | grep -oP '(?<="id":)[0-9]*')
echo "Mon ID utilisateur : $MY_ID"

# 4. CHANGE PHOTO DE PROFIL
echo -e "\n[4] Mise à jour photo de profil..."
curl -X POST "$BASE_URL/changePP" -H "Authorization: $TOKEN" -F "profile_picture=@$IMAGE"

# 5. MAKE GROUP
echo -e "\n[5] Création d'un groupe privé..."
GRP_RES=$(curl -s -X POST "$BASE_URL/makeGroup" -H "Authorization: $TOKEN" \
     -F "name=Secret Club" -F "is_private=true" -F "password=1234" \
     -F "description=Test Automatique" -F "image=@$IMAGE")
GRP_ID=$(echo $GRP_RES | grep -oP '(?<="group_id":)[0-9]*')
echo "Groupe créé (ID: $GRP_ID)"

# 6. GET GROUPS (Public)
echo -e "\n[6] Liste de tous les groupes..."
curl -X GET "$BASE_URL/getGroups"

# 7. JOIN GROUP
echo -e "\n[7] Rejoint le groupe (auto-test)..."
curl -X POST "$BASE_URL/joinGroup" -H "Authorization: $TOKEN" \
     -F "group_id=$GRP_ID" -F "password=1234"

# 8. MAKE POST (Dans le groupe)
echo -e "\n[8] Création d'une publication..."
curl -X POST "$BASE_URL/makepost" -H "Authorization: $TOKEN" \
     -F "description=Ceci est un test complet" -F "id_loc=1" \
     -F "groupe=$GRP_ID" -F "image=@$IMAGE"

# 9. GET POSTS BY GROUP
echo -e "\n[9] Récupération des posts du groupe $GRP_ID..."
POST_RES=$(curl -s -X GET "$BASE_URL/getPostsByGroup?groupe=$GRP_ID" -H "Authorization: $TOKEN")
POST_ID=$(echo $POST_RES | grep -oP '(?<="id":)[0-9]*' | head -n 1)
echo "Dernier Post ID: $POST_ID"

# 10. GET SINGLE POST
echo -e "\n[10] Détails du post $POST_ID..."
curl -X GET "$BASE_URL/getpost?id=$POST_ID" -H "Authorization: $TOKEN"

# 11. FOLLOW / UNFOLLOW
echo -e "\n[11] Test Follow/Unfollow (sur soi-même)..."
curl -X POST "$BASE_URL/follow" -H "Authorization: $TOKEN" -F "user_id=$MY_ID"
curl -X GET "$BASE_URL/getFollowers" -H "Authorization: $TOKEN"
curl -X POST "$BASE_URL/unfollow" -H "Authorization: $TOKEN" -F "user_id=$MY_ID"

# 12. PUBLIC PROFILE
echo -e "\n[12] Profil public de l'utilisateur $MY_ID..."
curl -X GET "$BASE_URL/getPublicProfile?id=$MY_ID"

# 13. LOCALISATION
echo -e "\n[13] Test Localisation (ID 1)..."
curl -X GET "$BASE_URL/getLocations?id=1"

# 14. USER GROUPS
echo -e "\n[14] Test fetch groupes..."
curl -X GET http://mobile.piorian.fr/getUserGroups \
     -H "Authorization: $TOKEN"

# 15. LIKE / UNLIKE
echo -e "\n[15] Test Like/Unlike sur le post $POST_ID..."
curl -X POST "$BASE_URL/like" -H "Authorization: $TOKEN" -F "post_id=$POST_ID"
curl -X GET "$BASE_URL/getLikes?post_id=$POST_ID" -H "Authorization: $TOKEN"
curl -X GET "$BASE_URL/getAllUserLikes" -H "Authorization: $TOKEN"
curl -X POST "$BASE_URL/unlike" -H "Authorization: $TOKEN" -F "post_id=$POST_ID"

# 16. Commenter un post
echo -e "\n[16] Commenter le post $POST_ID..."
curl -X POST "$BASE_URL/comment" -H "Authorization: $TOKEN" -F "post_id=$POST_ID" -F "comment=Super post de test !"
curl -X GET "$BASE_URL/getComments?post_id=$POST_ID" -H "Authorization: $TOKEN"

# 17. Test Algorithme de recommandation
echo -e "\n[17] Test de l'algorithme de recommandation..."
curl -X GET "$BASE_URL/recommendations?gps=48.8566,2.3522" -H "Authorization: $TOKEN"

# 18. Test de signalement d'un post
echo -e "\n[18] Test de signalement du post $POST_ID..."
curl -X POST "$BASE_URL/report" -H "Authorization: $TOKEN" \
     -F "post_id=$POST_ID" -F "reason=Inapproprié" -F "commentaire=Ce post est offensant."

echo -e "\n\n=== TOUTES LES ROUTES ONT ÉTÉ CIBLÉES ==="
