-- Table des Utilisateurs
CREATE TABLE Utilisateurs (
    usr_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password TEXT NOT NULL,
    url_pp TEXT
);

-- Table des Localisations
CREATE TABLE Localisation (
    id_loc SERIAL PRIMARY KEY,
    nom VARCHAR(255),
    gps POINT 
);

-- Table des Publications
CREATE TABLE Publications (
    id_pub SERIAL PRIMARY KEY,
    id_publicateur INT REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE,
    groupe INT REFERENCES Groupes(id_com) ON DELETE SET NULL,
    description TEXT,
    id_localisation INT REFERENCES Localisation(id_loc),
    url_image TEXT NOT NULL,
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des Commentaires
CREATE TABLE Commentaires (
    id_com SERIAL PRIMARY KEY,
    id_publication INT REFERENCES Publications(id_pub) ON DELETE CASCADE,
    id_utilisateur INT REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE,
    commentaire TEXT NOT NULL
);

-- Table des Likes (Contrainte : un utilisateur ne peut liker qu'une fois une photo)
CREATE TABLE Likes (
    id_publication INT REFERENCES Publications(id_pub) ON DELETE CASCADE,
    id_utilisateur INT REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE,
    PRIMARY KEY (id_publication, id_utilisateur)
);

CREATE TABLE Groupes (
    id_com SERIAL PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    description TEXT
    PRIMARY KEY (id_publication, id_utilisateur)
);


INSERT INTO Localisation (id_loc, nom, gps) VALUES (1, 'Debug', '(43.6107, 3.8767)');
INSERT INTO Groupes (id_com, nom, description) VALUES (0, 'No Group', 'Default group for posts without a specific group');