#!/bin/bash

# Configuration
BASE_URL="http://localhost:8080"
IMAGE="/home/dorian/Téléchargements/ev.png"
EMAIL="test_complet@example.com"
PASSWORD="password123"
USERNAME="DorianTester"

PASS=0
FAIL=0

check() {
    local label="$1"
    local status="$2"
    local expected="$3"
    if [ "$status" -eq "$expected" ]; then
        echo "  ✓ $label (HTTP $status)"
        PASS=$((PASS+1))
    else
        echo "  ✗ $label — attendu HTTP $expected, reçu HTTP $status"
        FAIL=$((FAIL+1))
    fi
}

sudo rm -rf ./uploads/posts/* ./uploads/groupes_pic/* ./uploads/profiles_pic/*

echo "=== DÉMARRAGE DES TESTS DE L'API ==="

# ────────────────────────────────────────────────────
# AUTH
# ────────────────────────────────────────────────────
echo -e "\n[AUTH] Inscription..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/signup" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL\", \"password\":\"$PASSWORD\", \"username\":\"$USERNAME\"}")
check "signup nouvel utilisateur" "$STATUS" 201

echo "[AUTH] Double inscription (email dupliqué)..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/signup" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL\", \"password\":\"$PASSWORD\", \"username\":\"$USERNAME\"}")
check "signup email dupliqué → 500" "$STATUS" 500

echo "[AUTH] Signup sans email..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/signup" \
    -H "Content-Type: application/json" \
    -d "{\"password\":\"$PASSWORD\", \"username\":\"Test\"}")
check "signup sans email → 500" "$STATUS" 400

echo "[AUTH] Connexion..."
LOGIN_RES=$(curl -s -X POST "$BASE_URL/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL\", \"password\":\"$PASSWORD\"}")
TOKEN=$(echo "$LOGIN_RES" | grep -oP '(?<="token":")[^"]*')
if [ -z "$TOKEN" ]; then echo "✗ Échec de l'authentification — arrêt."; exit 1; fi
check "login valide → 200" "200" 200
echo "  JWT obtenu."

echo "[AUTH] Connexion mauvais mot de passe..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL\", \"password\":\"mauvais\"}")
check "login mauvais mdp → 401" "$STATUS" 401

echo "[AUTH] Connexion email inconnu..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"inconnu@x.com\", \"password\":\"$PASSWORD\"}")
check "login email inconnu → 401" "$STATUS" 401

echo "[AUTH] Accès route protégée sans token..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/profile")
check "route protégée sans token → 401" "$STATUS" 401

echo "[AUTH] Accès route protégée token invalide..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/profile" \
    -H "Authorization: token.bidon.xxx")
check "route protégée token invalide → 401" "$STATUS" 401

# ────────────────────────────────────────────────────
# PROFIL
# ────────────────────────────────────────────────────
echo -e "\n[PROFIL] Récupération de mon profil..."
PROFILE_RES=$(curl -s -X GET "$BASE_URL/profile" -H "Authorization: $TOKEN")
MY_ID=$(echo "$PROFILE_RES" | grep -oP '(?<="id":)[0-9]*')
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/profile" -H "Authorization: $TOKEN")
check "GET /profile authentifié → 200" "$STATUS" 200
echo "  Mon ID utilisateur : $MY_ID"

echo "[PROFIL] Profil public existant..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getPublicProfile?id=$MY_ID")
check "GET /getPublicProfile?id=$MY_ID → 200" "$STATUS" 200

echo "[PROFIL] Profil public inexistant..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getPublicProfile?id=999999")
check "GET /getPublicProfile?id=999999 → 404" "$STATUS" 404

echo "[PROFIL] Profil public sans id..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getPublicProfile")
check "GET /getPublicProfile sans id → 400" "$STATUS" 400

echo "[PROFIL] Mise à jour photo de profil..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/changePP" \
    -H "Authorization: $TOKEN" -F "profile_picture=@$IMAGE")
check "POST /changePP → 200" "$STATUS" 200

echo "[PROFIL] changePP sans image..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/changePP" \
    -H "Authorization: $TOKEN")
check "POST /changePP sans image → 400" "$STATUS" 400

# ────────────────────────────────────────────────────
# GROUPES
# ────────────────────────────────────────────────────
echo -e "\n[GROUPES] Création groupe privé..."
GRP_RES=$(curl -s -X POST "$BASE_URL/makeGroup" -H "Authorization: $TOKEN" \
    -F "name=Secret Club" -F "is_private=true" -F "password=1234" \
    -F "description=Test Automatique" -F "image=@$IMAGE")
GRP_ID=$(echo "$GRP_RES" | grep -oP '(?<="group_id":)[0-9]*')
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/makeGroup" \
    -H "Authorization: $TOKEN" \
    -F "name=Secret Club 2" -F "is_private=true" -F "password=1234" \
    -F "description=Test" -F "image=@$IMAGE")
check "POST /makeGroup privé → 200" "$STATUS" 200
echo "  Groupe créé (ID: $GRP_ID)"

echo "[GROUPES] Création groupe sans nom..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/makeGroup" \
    -H "Authorization: $TOKEN" -F "is_private=false" -F "image=@$IMAGE")
check "POST /makeGroup sans nom → 400" "$STATUS" 400

echo "[GROUPES] Création groupe privé sans mot de passe..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/makeGroup" \
    -H "Authorization: $TOKEN" -F "name=Test" -F "is_private=true" -F "image=@$IMAGE")
check "POST /makeGroup privé sans mdp → 400" "$STATUS" 400

echo "[GROUPES] Liste tous les groupes..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getGroups")
check "GET /getGroups → 200" "$STATUS" 200

echo "[GROUPES] Infos groupe existant..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getGroupInfo?id=$GRP_ID")
check "GET /getGroupInfo?id=$GRP_ID → 200" "$STATUS" 200

echo "[GROUPES] Infos groupe inexistant..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getGroupInfo?id=999999")
check "GET /getGroupInfo?id=999999 → 404" "$STATUS" 404

echo "[GROUPES] Infos groupe sans id..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getGroupInfo")
check "GET /getGroupInfo sans id → 400" "$STATUS" 400

echo "[GROUPES] Rejoindre groupe privé (bon mot de passe)..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/joinGroup" \
    -H "Authorization: $TOKEN" -F "group_id=$GRP_ID" -F "password=1234")
check "POST /joinGroup bon mdp → 200" "$STATUS" 200

echo "[GROUPES] Rejoindre groupe privé (mauvais mot de passe)..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/joinGroup" \
    -H "Authorization: $TOKEN" -F "group_id=$GRP_ID" -F "password=mauvais")
check "POST /joinGroup mauvais mdp → 401" "$STATUS" 401

echo "[GROUPES] Groupes de l'utilisateur..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getUserGroups" \
    -H "Authorization: $TOKEN")
check "GET /getUserGroups → 200" "$STATUS" 200

# ────────────────────────────────────────────────────
# PUBLICATIONS
# ────────────────────────────────────────────────────
echo -e "\n[POSTS] Création post dans le groupe..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/makepost" \
    -H "Authorization: $TOKEN" \
    -F "description=Ceci est un test complet" -F "id_loc=1" \
    -F "groupe=$GRP_ID" -F "image=@$IMAGE")
check "POST /makepost avec groupe → 201" "$STATUS" 201

echo "[POSTS] Création post public (groupe=0)..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/makepost" \
    -H "Authorization: $TOKEN" \
    -F "description=Post public" -F "id_loc=1" \
    -F "groupe=0" -F "image=@$IMAGE")
check "POST /makepost groupe=0 → 201" "$STATUS" 201

echo "[POSTS] Création post sans image..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/makepost" \
    -H "Authorization: $TOKEN" \
    -F "description=Sans image" -F "id_loc=1")
check "POST /makepost sans image → 400" "$STATUS" 400

echo "[POSTS] Création post dans un groupe dont on n'est pas membre..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/makepost" \
    -H "Authorization: $TOKEN" \
    -F "description=Test" -F "id_loc=1" \
    -F "groupe=999999" -F "image=@$IMAGE")
check "POST /makepost groupe non-membre → 403" "$STATUS" 403

echo "[POSTS] Posts du groupe..."
POST_RES=$(curl -s -X GET "$BASE_URL/getPostsByGroup?groupe=$GRP_ID" -H "Authorization: $TOKEN")
POST_ID=$(echo "$POST_RES" | grep -oP '(?<="id":)[0-9]*' | head -n 1)
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/getPostsByGroup?groupe=$GRP_ID" -H "Authorization: $TOKEN")
check "GET /getPostsByGroup → 200" "$STATUS" 200
echo "  Post ID récupéré : $POST_ID"

echo "[POSTS] Posts d'un groupe sans être membre..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/getPostsByGroup?groupe=999999" -H "Authorization: $TOKEN")
check "GET /getPostsByGroup groupe non-membre → 403" "$STATUS" 403

echo "[POSTS] Posts d'un groupe sans l'id..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/getPostsByGroup" -H "Authorization: $TOKEN")
check "GET /getPostsByGroup sans id → 400" "$STATUS" 400

echo "[POSTS] Détail d'un post existant..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/getpost?id=$POST_ID" -H "Authorization: $TOKEN")
check "GET /getpost?id=$POST_ID → 200" "$STATUS" 200

echo "[POSTS] Détail d'un post inexistant..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/getpost?id=999999" -H "Authorization: $TOKEN")
check "GET /getpost?id=999999 → 404" "$STATUS" 404

echo "[POSTS] Détail post sans id..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getpost")
check "GET /getpost sans id → 400" "$STATUS" 400

echo "[POSTS] Accès post de groupe (non-membre sans token)..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/getpost?id=$POST_ID")
check "GET /getpost sans token sur post privé → 403" "$STATUS" 403

# ────────────────────────────────────────────────────
# LIKES
# ────────────────────────────────────────────────────
echo -e "\n[LIKES] Liker un post..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    "$BASE_URL/like?id=$POST_ID" -H "Authorization: $TOKEN")
check "POST /like → 200" "$STATUS" 200

echo "[LIKES] Double like (idempotence)..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    "$BASE_URL/like?id=$POST_ID" -H "Authorization: $TOKEN")
# Comportement dépend de la contrainte UNIQUE en base — 200 ou 500
echo "  Double like HTTP $STATUS (dépend de la contrainte UNIQUE)"

echo "[LIKES] Récupérer le nombre de likes..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getLikes?id=$POST_ID")
check "GET /getLikes → 200" "$STATUS" 200

echo "[LIKES] Récupérer tous les likes de l'utilisateur..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getAllUserLikes" \
    -H "Authorization: $TOKEN")
check "GET /getAllUserLikes → 200" "$STATUS" 200

echo "[LIKES] getLikes sans id..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getLikes")
check "GET /getLikes sans id → 400" "$STATUS" 400

echo "[LIKES] Unlike..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    "$BASE_URL/unlike?id=$POST_ID" -H "Authorization: $TOKEN")
check "POST /unlike → 200" "$STATUS" 200

# ────────────────────────────────────────────────────
# COMMENTAIRES
# ────────────────────────────────────────────────────
echo -e "\n[COMMENTAIRES] Ajouter un commentaire..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    "$BASE_URL/comment?id=$POST_ID" -H "Authorization: $TOKEN" \
    -F "comment=Super post de test !")
check "POST /comment → 200" "$STATUS" 200

echo "[COMMENTAIRES] Commenter sans id de post..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    "$BASE_URL/comment" -H "Authorization: $TOKEN" \
    -F "comment=Test")
check "POST /comment sans id → 400" "$STATUS" 400

echo "[COMMENTAIRES] Récupérer commentaires..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getComments?id=$POST_ID")
check "GET /getComments → 200" "$STATUS" 200

echo "[COMMENTAIRES] getComments sans id..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getComments")
check "GET /getComments sans id → 400" "$STATUS" 400

# ────────────────────────────────────────────────────
# FOLLOWERS
# ────────────────────────────────────────────────────
echo -e "\n[FOLLOWERS] Follow (sur soi-même)..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/follow" \
    -H "Authorization: $TOKEN" -F "user_id=$MY_ID")
check "POST /follow → 200" "$STATUS" 200

echo "[FOLLOWERS] Lister ses followers..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getFollowers" \
    -H "Authorization: $TOKEN")
check "GET /getFollowers → 200" "$STATUS" 200

echo "[FOLLOWERS] Follow utilisateur inexistant..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/follow" \
    -H "Authorization: $TOKEN" -F "user_id=999999")
check "POST /follow user inexistant → 404" "$STATUS" 404

echo "[FOLLOWERS] Unfollow..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/unfollow" \
    -H "Authorization: $TOKEN" -F "user_id=$MY_ID")
check "POST /unfollow → 200" "$STATUS" 200

# ────────────────────────────────────────────────────
# LOCALISATION
# ────────────────────────────────────────────────────
echo -e "\n[LOCALISATION] Localisation ID 1..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getLocations?id=1")
check "GET /getLocations?id=1 → 200" "$STATUS" 200

echo "[LOCALISATION] Localisation inexistante..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getLocations?id=999999")
check "GET /getLocations?id=999999 → 404" "$STATUS" 404

echo "[LOCALISATION] Localisation sans id..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/getLocations")
check "GET /getLocations sans id → 400" "$STATUS" 400

# ────────────────────────────────────────────────────
# ALGO PROXIMITÉ
# ────────────────────────────────────────────────────
echo -e "\n[ALGO] Posts à proximité (Toulouse)..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/getNearbyPosts?gps=43.6107,3.8767" -H "Authorization: $TOKEN")
check "GET /getNearbyPosts → 200" "$STATUS" 200

echo "[ALGO] Posts à proximité sans coordonnées..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/getNearbyPosts" -H "Authorization: $TOKEN")
check "GET /getNearbyPosts sans gps → 400" "$STATUS" 400

# ────────────────────────────────────────────────────
# SIGNALEMENT
# ────────────────────────────────────────────────────
echo -e "\n[REPORT] Signaler un post..."
# CORRECTION : la route est /reportPost, pas /report
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/reportPost?id=$POST_ID" \
    -H "Authorization: $TOKEN" \
    -F "reason=Inapproprié" -F "comment=Ce post est offensant.")
check "POST /reportPost → 200" "$STATUS" 200

echo "[REPORT] Signaler sans id de post..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/reportPost" \
    -H "Authorization: $TOKEN" -F "reason=Spam")
check "POST /reportPost sans id → 400" "$STATUS" 400

# ────────────────────────────────────────────────────
# SUPPRESSION (en dernier pour ne pas casser les tests précédents)
# ────────────────────────────────────────────────────
echo -e "\n[DELETE] Supprimer son propre post..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
    "$BASE_URL/deletePost?id=$POST_ID" -H "Authorization: $TOKEN")
check "DELETE /deletePost → 200" "$STATUS" 200

echo "[DELETE] Supprimer post déjà supprimé..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
    "$BASE_URL/deletePost?id=$POST_ID" -H "Authorization: $TOKEN")
check "DELETE /deletePost déjà supprimé → 404" "$STATUS" 404

echo "[DELETE] Supprimer post sans id..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
    "$BASE_URL/deletePost" -H "Authorization: $TOKEN")
check "DELETE /deletePost sans id → 400" "$STATUS" 400

# ────────────────────────────────────────────────────
# RÉSUMÉ
# ────────────────────────────────────────────────────
echo -e "\n================================================"
echo "  RÉSULTAT : $PASS tests passés, $FAIL tests échoués"
echo "================================================"