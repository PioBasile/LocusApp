-- Groupes FIRST since Publications references it
CREATE TABLE Groupes (
    id_grp SERIAL PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    description TEXT
);

-- Utilisateurs
CREATE TABLE Utilisateurs (
    usr_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password TEXT NOT NULL,
    url_pp TEXT
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
    groupe INT REFERENCES Groupes(id_grp) ON DELETE SET NULL,
    description TEXT,
    id_localisation INT REFERENCES Localisation(id_loc),
    url_image TEXT NOT NULL,
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Commentaires (
    id_com SERIAL PRIMARY KEY,
    id_publication INT REFERENCES Publications(id_pub) ON DELETE CASCADE,
    id_utilisateur INT REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE,
    commentaire TEXT NOT NULL
);

CREATE TABLE Likes (
    id_publication INT REFERENCES Publications(id_pub) ON DELETE CASCADE,
    id_utilisateur INT REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE,
    PRIMARY KEY (id_publication, id_utilisateur)
);

INSERT INTO Localisation (nom, gps) VALUES ('Debug', '(43.6107, 3.8767)');
INSERT INTO Groupes (nom, description) VALUES ('No Group', 'Default group for posts without a specific group');