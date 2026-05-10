-- ============================================================
-- init.sql — TravelShare / Locus
-- ============================================================

-- Utilisateurs
CREATE TABLE Utilisateurs (
    usr_id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password TEXT NOT NULL,
    url_pp TEXT,
    fcm_token TEXT
);

-- Groupes
CREATE TABLE Groupes (
    id_grp SERIAL PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    is_private BOOLEAN DEFAULT FALSE,
    password TEXT,
    owner_id INT REFERENCES Utilisateurs(usr_id) ON DELETE SET NULL,
    description TEXT,
    url_image TEXT
);

CREATE TABLE MembreGroupes (
    id_grp INT REFERENCES Groupes(id_grp) ON DELETE CASCADE,
    usr_id INT REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE,
    PRIMARY KEY (id_grp, usr_id)
);

-- Localisation
CREATE TABLE Localisation (
    id_loc SERIAL PRIMARY KEY,
    nom VARCHAR(255),
    gps POINT
);

-- Publications
CREATE TABLE Publications (
    id_pub SERIAL PRIMARY KEY,
    id_publicateur INT REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE,
    description TEXT,
    id_localisation INT REFERENCES Localisation(id_loc),
    url_image TEXT NOT NULL,
    url_audio TEXT,
    tags TEXT[],
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE PublicationGroupe (
    id_pub INT REFERENCES Publications(id_pub) ON DELETE CASCADE,
    id_grp INT REFERENCES Groupes(id_grp) ON DELETE CASCADE,
    PRIMARY KEY (id_pub, id_grp)
);

CREATE TABLE Commentaires (
    id_com SERIAL PRIMARY KEY,
    id_pub INT REFERENCES Publications(id_pub) ON DELETE CASCADE,
    id_user INT REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE,
    url_audio TEXT,
    commentaire TEXT NOT NULL
);

CREATE TABLE Likes (
    id_like SERIAL PRIMARY KEY,
    id_pub INT REFERENCES Publications(id_pub) ON DELETE CASCADE,
    id_user INT REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE
);

CREATE TABLE Reports (
    id_report SERIAL PRIMARY KEY,
    id_publication INT REFERENCES Publications(id_pub) ON DELETE CASCADE,
    id_utilisateur INT REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    commentaire TEXT
);

CREATE TABLE Followers (
    follower_id INT REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE,
    followed_id INT REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE,
    PRIMARY KEY (follower_id, followed_id)
);

-- ============================================================
-- LIEUX (établissements géolocalisés)
-- ============================================================

CREATE TYPE lieu_categorie AS ENUM (
    'restaurant', 'bar', 'cafe', 'musee', 'monument', 'parc',
    'shopping', 'sport', 'hotel', 'plage', 'autre'
);

CREATE TABLE Lieux (
    id_lieu     SERIAL PRIMARY KEY,
    nom         VARCHAR(255) NOT NULL,
    description TEXT,
    adresse     VARCHAR(500),
    categorie   lieu_categorie DEFAULT 'autre',
    gps         POINT NOT NULL,
    url_image   TEXT,
    note        FLOAT DEFAULT 0,
    nb_avis     INT DEFAULT 0,
    horaires    TEXT,
    prix_moyen  INT DEFAULT 0,
    site_web    TEXT,
    telephone   VARCHAR(30),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_loc      INT REFERENCES Localisation(id_loc) ON DELETE SET NULL
);

CREATE INDEX idx_lieux_categorie ON Lieux (categorie);

CREATE TABLE LieuxPhotos (
    id_photo SERIAL PRIMARY KEY,
    id_lieu  INT REFERENCES Lieux(id_lieu) ON DELETE CASCADE,
    url      TEXT NOT NULL,
    legende  TEXT,
    ordre    INT DEFAULT 0
);

CREATE TABLE LieuxAvis (
    id_avis      SERIAL PRIMARY KEY,
    id_lieu      INT REFERENCES Lieux(id_lieu) ON DELETE CASCADE,
    usr_id       INT REFERENCES Utilisateurs(usr_id) ON DELETE SET NULL,
    note         INT CHECK (note BETWEEN 1 AND 5),
    commentaire  TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Itineraires (
    id_itin    SERIAL PRIMARY KEY,
    usr_id     INT REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE,
    nom        VARCHAR(255),
    type       VARCHAR(50),
    budget     INT,
    duree_min  INT,
    effort     INT,
    donnees    JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ItinerairesLikes (
    id_like SERIAL PRIMARY KEY,
    usr_id INTEGER REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE,
    id_itin INTEGER REFERENCES Itineraires(id_itin) ON DELETE CASCADE,
    UNIQUE(usr_id, id_itin)
);

-- ============================================================
-- DONNÉES DE BASE
-- ============================================================

INSERT INTO Localisation (nom, gps) VALUES ('Debug', '(43.6107, 3.8767)');
INSERT INTO Groupes (id_grp, nom, is_private, password, owner_id, description) VALUES (0, 'Default', FALSE, NULL, NULL, 'Pas de groupe');
INSERT INTO Utilisateurs (usr_id, username, email, password, url_pp) VALUES (0, 'debug_user', 'debug@test.com', 'password', 'img.jpg');
INSERT INTO Utilisateurs (usr_id, username, email, password, url_pp) VALUES (-1, 'Anonyme', 'Anonyme', 'password', 'img.jpg');

-- ============================================================
-- LIEUX DE MONTPELLIER
-- ============================================================

-- Restaurants
INSERT INTO Lieux (nom, description, adresse, categorie, gps, note, nb_avis, horaires, prix_moyen) VALUES
('Le Petit Jardin', 'Restaurant gastronomique avec jardin intérieur, cuisine du terroir occitan.', '20 Rue Jean-Jacques Rousseau, 34000 Montpellier', 'restaurant', '(43.6112, 3.8741)', 4.5, 287, 'Lun-Sam 12h-14h / 19h-22h', 45),
('La Diligence', 'Cuisine traditionnelle française dans un cadre historique du XVIIe siècle.', '2 Place Pétrarque, 34000 Montpellier', 'restaurant', '(43.6119, 3.8744)', 4.3, 198, 'Mar-Dim 12h-14h / 19h-22h30', 35),
('Tamarillos', 'Restaurant créatif et coloré, cuisine fusion méditerranéenne.', '2 Place du Marché aux Fleurs, 34000 Montpellier', 'restaurant', '(43.6088, 3.8769)', 4.4, 312, 'Lun-Sam 12h-14h30 / 19h30-22h30', 40),
('L''Artichaut', 'Bistronomie moderne, produits locaux et bio.', '13 Rue Candolle, 34000 Montpellier', 'restaurant', '(43.6097, 3.8799)', 4.6, 156, 'Mar-Sam 12h-14h / 19h30-22h', 38),
('Bodega Chez Boris', 'Tapas espagnoles, ambiance festive, vins naturels.', '9 Rue de la Carbonnerie, 34000 Montpellier', 'restaurant', '(43.6113, 3.8768)', 4.2, 423, 'Tous les jours 18h-minuit', 25),
('La Tomate', 'Pizzeria napolitaine authentique, four à bois.', '8 Rue des Balances, 34000 Montpellier', 'restaurant', '(43.6108, 3.8753)', 4.1, 534, 'Lun-Sam 11h30-14h30 / 18h30-22h30', 18),
('Café de la Mer', 'Poissons et fruits de mer frais, terrasse face aux Halles Castellane.', '1 Place de la Chapelle Neuve, 34000 Montpellier', 'restaurant', '(43.6095, 3.8762)', 4.3, 289, 'Mar-Dim 12h-15h / 19h-23h', 42),
('Kokoa', 'Restaurant coréen, bibimbap et barbecue maison.', '5 Rue Saint-Guilhem, 34000 Montpellier', 'restaurant', '(43.6116, 3.8772)', 4.4, 178, 'Mar-Dim 12h-14h30 / 19h-22h', 22);

-- Bars & Cafés
INSERT INTO Lieux (nom, description, adresse, categorie, gps, note, nb_avis, horaires, prix_moyen) VALUES
('Le Fitzpatrick', 'Bar irlandais incontournable, Guinness pression et concerts live.', '7 Rue Aristide Ollivier, 34000 Montpellier', 'bar', '(43.6101, 3.8776)', 4.5, 621, 'Lun-Dim 12h-2h', 12),
('Café de la Placette', 'Café de quartier, terrasse ombragée, bière artisanale locale.', '4 Place de la Canourgue, 34000 Montpellier', 'cafe', '(43.6118, 3.8748)', 4.2, 203, 'Mar-Dim 8h-19h', 8),
('Le Rockstore', 'Bar rock mythique, scène live, concerts plusieurs soirs/semaine.', '20 Rue de Verdun, 34000 Montpellier', 'bar', '(43.6089, 3.8797)', 4.3, 1245, 'Mer-Dim 20h-3h', 10),
('L''Heure Bleue', 'Cocktails créatifs, bar à vin, ambiance lounge intimiste.', '12 Rue de l''Aiguillerie, 34000 Montpellier', 'bar', '(43.6103, 3.8751)', 4.6, 312, 'Mar-Sam 18h-2h', 14),
('Café de la Comédie', 'Café brasserie en face de l''Opéra Comédie, terrasse animée.', '1 Place de la Comédie, 34000 Montpellier', 'cafe', '(43.6088, 3.8783)', 3.9, 892, 'Lun-Dim 7h-23h', 9),
('Café Joseph', 'Coffee shop indépendant, spécialités de café, brunch le weekend.', '14 Rue Joseph Lakanal, 34000 Montpellier', 'cafe', '(43.6115, 3.8733)', 4.7, 445, 'Mar-Ven 8h-18h / Sam-Dim 9h-17h', 7);

-- Musées & Culture
INSERT INTO Lieux (nom, description, adresse, categorie, gps, note, nb_avis, horaires, prix_moyen) VALUES
('Musée Fabre', 'L''un des plus importants musées de beaux-arts de France, collections du XIVe au XXIe siècle.', '39 Bd Bonne Nouvelle, 34000 Montpellier', 'musee', '(43.6118, 3.8820)', 4.7, 2341, 'Mar-Dim 10h-18h (Jeu jusqu''à 20h)', 0),
('MOCO - Montpellier Contemporain', 'Art contemporain international dans un bâtiment rénové par Kengo Kuma.', '13 Rue de la République, 34000 Montpellier', 'musee', '(43.6112, 3.8825)', 4.4, 876, 'Mar-Dim 11h-19h', 8),
('Musée de l''Agropolis', 'Musée unique dédié aux plantes cultivées et aux agricultures du monde.', '2271 Av. du Val de Montferrand, 34000 Montpellier', 'musee', '(43.6371, 3.8556)', 4.3, 543, 'Mar-Sam 10h-18h', 5),
('Planétarium Galilée', 'Séances d''astronomie immersives sous coupole.', 'Bd des Caousines, 34000 Montpellier', 'musee', '(43.6287, 3.8523)', 4.5, 678, 'Mer/Sam/Dim séances à 15h et 17h', 7),
('Opéra Comédie', 'Opéra historique du XIXe siècle, programmation lyrique et ballet.', '11 Bd Victor Hugo, 34000 Montpellier', 'monument', '(43.6087, 3.8786)', 4.8, 1123, 'Selon programmation', 20);

-- Monuments & Sites
INSERT INTO Lieux (nom, description, adresse, categorie, gps, note, nb_avis, horaires, prix_moyen) VALUES
('Place de la Comédie', 'Place centrale de Montpellier, l''Œuf, fontaine des Trois Grâces.', 'Place de la Comédie, 34000 Montpellier', 'monument', '(43.6088, 3.8783)', 4.5, 5421, 'Accès libre 24h/24', 0),
('Arc de Triomphe', 'Arc de triomphe du XVIIe siècle, porte d''entrée de l''Écusson.', 'Rue Foch, 34000 Montpellier', 'monument', '(43.6095, 3.8730)', 4.4, 2341, 'Extérieur : accès libre', 0),
('Promenade du Peyrou', 'Esplanade royale avec château d''eau, vue panoramique sur la ville.', 'Place du Peyrou, 34000 Montpellier', 'monument', '(43.6130, 3.8697)', 4.6, 3456, 'Lun-Dim 6h-22h', 0),
('Cathédrale Saint-Pierre', 'Cathédrale gothique du XIVe siècle, imposante façade avec deux tours.', 'Place Saint-Pierre, 34000 Montpellier', 'monument', '(43.6122, 3.8756)', 4.5, 1876, 'Lun-Dim 8h-19h', 0),
('Faculté de Médecine', 'La plus ancienne faculté de médecine d''Europe (1220), musée d''anatomie.', '2 Rue de l''École de Médecine, 34000 Montpellier', 'monument', '(43.6118, 3.8764)', 4.7, 789, 'Visite guidée uniquement', 0),
('Aqueduc Saint-Clément (Arceaux)', 'Aqueduc du XVIIIe siècle, marché bio sous les arches le mardi et samedi.', 'Av. Samuel de Champlain, 34000 Montpellier', 'monument', '(43.6149, 3.8691)', 4.6, 2134, 'Accès libre / Marché : Mar et Sam matin', 0),
('Quartier Antigone', 'Quartier néo-classique dessiné par Ricardo Bofill dans les années 80.', 'Place du Nombre d''Or, 34000 Montpellier', 'monument', '(43.6061, 3.8876)', 4.1, 1234, 'Accès libre', 0);

-- Parcs & Nature
INSERT INTO Lieux (nom, description, adresse, categorie, gps, note, nb_avis, horaires, prix_moyen) VALUES
('Jardin des Plantes', 'Le plus ancien jardin botanique de France (1593), herbier de 2 millions de planches.', '163 Rue Auguste Broussonet, 34000 Montpellier', 'parc', '(43.6131, 3.8771)', 4.6, 3456, 'Mar-Dim 12h-20h (été) / 12h-18h (hiver)', 0),
('Domaine de Méric', 'Château du XVIIe siècle entouré de jardins à la française.', 'Rte de Lavérune, 34070 Montpellier', 'parc', '(43.5987, 3.8401)', 4.3, 876, 'Lun-Dim 8h-19h', 0),
('Parc Zoologique de Montpellier', 'Zoo gratuit, l''un des plus importants de France avec 160 espèces.', '50 Av. Agropolis, 34090 Montpellier', 'parc', '(43.6349, 3.8615)', 4.7, 8765, 'Tous les jours 9h-19h (été) / 9h-17h (hiver)', 0),
('Plage de Palavas-les-Flots', 'Plage populaire à 12km de Montpellier, sable fin, animations estivales.', 'Palavas-les-Flots, 34250', 'plage', '(43.5290, 3.9285)', 4.1, 12453, 'Accès libre', 0),
('Plage de la Grande-Motte', 'Station balnéaire avec architecture pyramidale unique, port de plaisance.', 'La Grande-Motte, 34280', 'plage', '(43.5601, 4.0845)', 4.3, 15678, 'Accès libre', 0);

-- Shopping
INSERT INTO Lieux (nom, description, adresse, categorie, gps, note, nb_avis, horaires, prix_moyen) VALUES
('Les Halles Castellane', 'Marché couvert, fromages, charcuteries, fruits et légumes régionaux.', '1 Rue de la Loge, 34000 Montpellier', 'shopping', '(43.6095, 3.8770)', 4.5, 2341, 'Lun-Dim 7h-14h', 0),
('Marché du Lez', 'Marché créatif et populaire : artisans, street food, vintage, concerts.', '635 Av. de la Mer, 34000 Montpellier', 'shopping', '(43.5981, 3.9025)', 4.6, 5678, 'Sam 9h-18h / Dim 9h-16h', 0);

-- Sport & Loisirs
INSERT INTO Lieux (nom, description, adresse, categorie, gps, note, nb_avis, horaires, prix_moyen) VALUES
('Climbing District', 'Salle d''escalade indoor XXL, bloc et voie.', '157 Rue Alexandre Fleming, 34000 Montpellier', 'sport', '(43.5812, 3.8834)', 4.7, 543, 'Lun-Ven 10h-22h / Sam-Dim 10h-20h', 12),
('Arena Montpellier', 'Grande salle de concert et de sport, matchs de handball (MHB).', 'Av. Albert Einstein, 34000 Montpellier', 'sport', '(43.5892, 3.9015)', 4.4, 4532, 'Selon événements', 25);

-- Hôtels
INSERT INTO Lieux (nom, description, adresse, categorie, gps, note, nb_avis, horaires, prix_moyen) VALUES
('Hôtel du Palais', 'Boutique hôtel 4* en plein cœur de l''Écusson, charme historique.', '3 Rue du Palais des Guilhem, 34000 Montpellier', 'hotel', '(43.6108, 3.8757)', 4.6, 892, 'Réception 24h/24', 120),
('Pullman Montpellier Centre', 'Hôtel 4* business & leisure, rooftop avec vue, piscine.', '1 Rue des Pertuisanes, 34000 Montpellier', 'hotel', '(43.6071, 3.8891)', 4.3, 1456, 'Réception 24h/24', 180);

-- Créer les entrées Localisation correspondantes pour les lieux principaux (monuments, musées, parcs)
INSERT INTO Localisation (nom, gps)
SELECT nom, gps FROM Lieux WHERE categorie IN ('monument', 'musee', 'parc');

-- Lier les lieux à leurs localisations
UPDATE Lieux l
SET id_loc = loc.id_loc
FROM Localisation loc
WHERE loc.nom = l.nom
  AND l.id_loc IS NULL;