-- Utilisateurs
CREATE TABLE Utilisateurs (
    usr_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password TEXT NOT NULL,
    url_pp TEXT
);

-- Groupes FIRST since Publications references it
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
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE PublicationGroupe (
    id_pub INT REFERENCES Publications(id_pub) ON DELETE CASCADE,
    id_grp INT REFERENCES Groupes(id_grp) ON DELETE CASCADE,
    PRIMARY KEY (id_pub, id_grp)
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

CREATE TABLE Followers (
    follower_id INT REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE,
    followed_id INT REFERENCES Utilisateurs(usr_id) ON DELETE CASCADE,
    PRIMARY KEY (follower_id, followed_id)
);

INSERT INTO Localisation (nom, gps) VALUES ('Debug', '(43.6107, 3.8767)');
INSERT INTO Groupes (id_grp, nom, is_private, password, owner_id, description) VALUES (0, 'Default', FALSE, NULL, NULL, 'Pas de groupe');
INSERT INTO Utilisateurs (usr_id, username, email, password, url_pp) VALUES (0, 'debug_user', 'debug@test.com', 'password', 'img.jpg');