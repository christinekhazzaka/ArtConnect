use ArtConnect;

CREATE TABLE UserAccount(
   user_id VARCHAR(50),
   email VARCHAR(50) NOT NULL,
   name VARCHAR(50) NOT NULL,
   password VARCHAR(255) NOT NULL,
   role VARCHAR(50) NOT NULL,
   PRIMARY KEY(user_id),
   UNIQUE(email)
);

CREATE TABLE CommunityMember(
   member_id VARCHAR(50),
   join_date DATE NOT NULL,
   city VARCHAR(100) NOT NULL,
   membership_type VARCHAR(100) NOT NULL,
   user_id VARCHAR(50) NOT NULL,
   PRIMARY KEY(member_id),
   UNIQUE(user_id),
   FOREIGN KEY(user_id) REFERENCES UserAccount(user_id)
);

CREATE TABLE Artist(
   artist_id VARCHAR(50),
   biography TEXT NOT NULL,
   discipline VARCHAR(50) NOT NULL,
   birth_year INT NOT NULL,
   city VARCHAR(100) NOT NULL,
   user_id VARCHAR(50) NOT NULL,
   PRIMARY KEY(artist_id),
   UNIQUE(user_id),
   FOREIGN KEY(user_id) REFERENCES UserAccount(user_id)
);

CREATE TABLE Administrator(
   admin_id VARCHAR(50),
   admin_level VARCHAR(50),
   user_id VARCHAR(50) NOT NULL,
   PRIMARY KEY(admin_id),
   UNIQUE(user_id),
   FOREIGN KEY(user_id) REFERENCES UserAccount(user_id)
);

CREATE TABLE Artwork(
   artwork_id VARCHAR(50),
   title VARCHAR(50) NOT NULL,
   description TEXT NOT NULL,
   type VARCHAR(50) NOT NULL,
   creation_date DATE NOT NULL,
   artist_id VARCHAR(50) NOT NULL,
   PRIMARY KEY(artwork_id),
   UNIQUE(title),
   FOREIGN KEY(artist_id) REFERENCES Artist(artist_id)
);

CREATE TABLE Gallery(
   gallery_id VARCHAR(50),
   address VARCHAR(100) NOT NULL,
   name VARCHAR(100) NOT NULL,
   rating DOUBLE NOT NULL,
   admin_id VARCHAR(50) NOT NULL,
   PRIMARY KEY(gallery_id),
   FOREIGN KEY(admin_id) REFERENCES Administrator(admin_id)
);

CREATE TABLE Exhibition(
   exhibition_id VARCHAR(50),
   title VARCHAR(100) NOT NULL,
   description TEXT NOT NULL,
   event_date DATE NOT NULL,
   capacity INT NOT NULL,
   gallery_id VARCHAR(50) NOT NULL,
   PRIMARY KEY(exhibition_id),
   FOREIGN KEY(gallery_id) REFERENCES Gallery(gallery_id)
);

CREATE TABLE Workshop(
   workshop_id VARCHAR(50),
   title VARCHAR(100) NOT NULL,
   description TEXT NOT NULL,
   workshop_date DATE NOT NULL,
   location VARCHAR(100) NOT NULL,
   capacity INT NOT NULL,
   price DECIMAL(10,2) NOT NULL,
   level VARCHAR(100) NOT NULL,
   artist_id VARCHAR(50) NOT NULL,
   PRIMARY KEY(workshop_id),
   FOREIGN KEY(artist_id) REFERENCES Artist(artist_id)
);

CREATE TABLE WorkshopBooking(
   booking_id VARCHAR(50),
   booking_date DATETIME NOT NULL,
   payment_status VARCHAR(100) NOT NULL,
   member_id VARCHAR(50) NOT NULL,
   workshop_id VARCHAR(50) NOT NULL,
   PRIMARY KEY(booking_id),
   FOREIGN KEY(member_id) REFERENCES CommunityMember(member_id),
   FOREIGN KEY(workshop_id) REFERENCES Workshop(workshop_id)
);

CREATE TABLE Follows(
   member_id VARCHAR(50),
   artist_id VARCHAR(50),
   PRIMARY KEY(member_id, artist_id),
   FOREIGN KEY(member_id) REFERENCES CommunityMember(member_id),
   FOREIGN KEY(artist_id) REFERENCES Artist(artist_id)
);

CREATE TABLE presented_in(
   artwork_id VARCHAR(50),
   exhibition_id VARCHAR(50),
   PRIMARY KEY(artwork_id, exhibition_id),
   FOREIGN KEY(artwork_id) REFERENCES Artwork(artwork_id),
   FOREIGN KEY(exhibition_id) REFERENCES Exhibition(exhibition_id)
);

ALTER TABLE UserAccount
ADD CONSTRAINT chk_user_role
CHECK (role IN ('COMMUNITY_MEMBER', 'ARTIST', 'ADMINISTRATOR'));

ALTER TABLE Exhibition
ADD CONSTRAINT chk_exhibition_capacity
CHECK (capacity > 0);

ALTER TABLE Workshop
ADD CONSTRAINT chk_workshop_capacity
CHECK (capacity > 0);

ALTER TABLE WorkshopBooking
ADD CONSTRAINT chk_payment_status
CHECK (payment_status IN ('PENDING', 'PAID', 'CANCELLED', 'REFUNDED'));

ALTER TABLE Artwork
ADD CONSTRAINT chk_artwork_type
CHECK (type IN ('PAINTING', 'PHOTOGRAPHY', 'SCULPTURE', 'MUSIC', 'OTHER'));

ALTER TABLE Artist
ADD CONSTRAINT chk_artist_discipline
CHECK (discipline IN ('PAINTING', 'PHOTOGRAPHY', 'SCULPTURE', 'MUSIC', 'OTHER'));

ALTER TABLE CommunityMember
ADD CONSTRAINT chk_membership_type
CHECK (membership_type IN ('BASIC', 'PREMIUM'));

ALTER TABLE Workshop
ADD CONSTRAINT chk_workshop_level
CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'));

-- ------------------------------
-- UserAccount
-- ------------------------------
INSERT INTO UserAccount(user_id, email, name, password, role) VALUES
('U001','alice@example.com','Alice Smith','password123','COMMUNITY_MEMBER'),
('U002','bob@example.com','Bob Johnson','password123','COMMUNITY_MEMBER'),
('U003','carol@example.com','Carol Williams','password123','COMMUNITY_MEMBER'),
('U004','dave@example.com','Dave Brown','password123','COMMUNITY_MEMBER'),
('U005','emma@example.com','Emma Davis','password123','COMMUNITY_MEMBER'),
('U006','frank@example.com','Frank Wilson','password123','COMMUNITY_MEMBER'),
('U007','grace@example.com','Grace Lee','password123','COMMUNITY_MEMBER'),
('U008','henry@example.com','Henry Clark','password123','COMMUNITY_MEMBER'),
('U009','isabel@example.com','Isabel Adams','password123','COMMUNITY_MEMBER'),
('U010','jack@example.com','Jack Baker','password123','COMMUNITY_MEMBER'),
('U011','linda@example.com','Linda Carter','password123','COMMUNITY_MEMBER'),
('U012','michael@example.com','Michael Hall','password123','COMMUNITY_MEMBER'),
('U013','nina@example.com','Nina Young','password123','COMMUNITY_MEMBER'),
('U014','oliver@example.com','Oliver King','password123','COMMUNITY_MEMBER'),
('U015','paula@example.com','Paula Scott','password123','COMMUNITY_MEMBER'),
('U016','quentin@example.com','Quentin Wright','password123','COMMUNITY_MEMBER'),
('U017','rachel@example.com','Rachel Green','password123','COMMUNITY_MEMBER'),
('U018','steve@example.com','Steve Hill','password123','ARTIST'),
('U019','tina@example.com','Tina Moore','password123','ARTIST'),
('U020','victor@example.com','Victor Turner','password123','ADMINISTRATOR');

-- ------------------------------
-- CommunityMember
-- ------------------------------
INSERT INTO CommunityMember(member_id, join_date, city, membership_type, user_id) VALUES
('M001','2025-01-15','Paris','BASIC','U001'),
('M002','2025-01-20','Lyon','PREMIUM','U002'),
('M003','2025-01-22','Marseille','BASIC','U003'),
('M004','2025-01-25','Toulouse','BASIC','U004'),
('M005','2025-02-01','Nice','PREMIUM','U005'),
('M006','2025-02-05','Nantes','BASIC','U006'),
('M007','2025-02-07','Strasbourg','PREMIUM','U007'),
('M008','2025-02-10','Bordeaux','BASIC','U008'),
('M009','2025-02-12','Lille','BASIC','U009'),
('M010','2025-02-15','Rennes','PREMIUM','U010'),
('M011','2025-02-18','Paris','BASIC','U011'),
('M012','2025-02-20','Lyon','PREMIUM','U012'),
('M013','2025-02-22','Marseille','BASIC','U013'),
('M014','2025-02-25','Toulouse','BASIC','U014'),
('M015','2025-03-01','Nice','PREMIUM','U015'),
('M016','2025-03-03','Nantes','BASIC','U016'),
('M017','2025-03-05','Strasbourg','PREMIUM','U017'),
('M018','2025-03-07','Bordeaux','BASIC','U018'),
('M019','2025-03-10','Lille','BASIC','U019'),
('M020','2025-03-12','Rennes','PREMIUM','U020');

-- ------------------------------
-- Artist
-- ------------------------------
INSERT INTO Artist(artist_id, biography, discipline, birth_year, city, user_id) VALUES
('A001','Abstract painter using bright colors','PAINTING',1985,'Paris','U018'),
('A002','Classical pianist performing live','MUSIC',1990,'Lyon','U019'),
('A003','Digital illustrator for games','OTHER',1995,'Marseille','U001'),
('A004','Contemporary sculptor','SCULPTURE',1982,'Toulouse','U002'),
('A005','Urban photographer capturing streets','PHOTOGRAPHY',1993,'Nice','U003'),
('A006','Oil painter for landscapes','PAINTING',1988,'Nantes','U004'),
('A007','Jazz musician','MUSIC',1991,'Strasbourg','U005'),
('A008','Ceramic sculptor','SCULPTURE',1980,'Bordeaux','U006'),
('A009','Watercolor artist','PAINTING',1996,'Lille','U007'),
('A010','Indie singer-songwriter','MUSIC',1994,'Rennes','U008'),
('A011','3D designer for animation','OTHER',1997,'Paris','U009'),
('A012','Installation artist with lights','OTHER',1989,'Lyon','U010'),
('A013','Street muralist','PAINTING',1992,'Marseille','U011'),
('A014','Classical violinist','MUSIC',1987,'Toulouse','U012'),
('A015','Portrait photographer','PHOTOGRAPHY',1995,'Nice','U013'),
('A016','Glass artist','SCULPTURE',1983,'Nantes','U014'),
('A017','Modern abstract painter','PAINTING',1991,'Strasbourg','U015'),
('A018','Electronic music producer','MUSIC',1998,'Bordeaux','U016'),
('A019','Comic artist','OTHER',1996,'Lille','U017'),
('A020','Admin artist sample','OTHER',1984,'Rennes','U020');

-- ------------------------------
-- Administrator
-- ------------------------------
INSERT INTO Administrator(admin_id, admin_level, user_id) VALUES
('AD001','SUPER','U020'),
('AD002','STANDARD','U005'),
('AD003','STANDARD','U010'),
('AD004','STANDARD','U015'),
('AD005','STANDARD','U018'),
('AD006','STANDARD','U001'),
('AD007','STANDARD','U002'),
('AD008','STANDARD','U003'),
('AD009','STANDARD','U004'),
('AD010','STANDARD','U006'),
('AD011','STANDARD','U007'),
('AD012','STANDARD','U008'),
('AD013','STANDARD','U009'),
('AD014','STANDARD','U011'),
('AD015','STANDARD','U012'),
('AD016','STANDARD','U013'),
('AD017','STANDARD','U014'),
('AD018','STANDARD','U016'),
('AD019','STANDARD','U017'),
('AD020','STANDARD','U019');

-- ------------------------------
-- Artwork
-- ------------------------------
INSERT INTO Artwork(artwork_id, title, description, type, creation_date, artist_id) VALUES
('AW001','Sunset Dreams','Abstract painting with warm colors','PAINTING','2024-12-10','A001'),
('AW002','Ocean Whispers','Cool-toned abstract painting','PAINTING','2025-01-05','A001'),
('AW003','Moonlight Sonata','Piano composition in C minor','MUSIC','2025-02-01','A002'),
('AW004','Digital Hero','Character illustration for games','OTHER','2025-01-15','A003'),
('AW005','Urban Shadows','Street photo series','PHOTOGRAPHY','2025-01-20','A005'),
('AW006','Landscape Serenity','Oil painting of mountains','PAINTING','2025-01-25','A006'),
('AW007','Jazz Evening','Live performance recording','MUSIC','2025-02-05','A007'),
('AW008','Clay Vase','Ceramic art','SCULPTURE','2025-02-10','A008'),
('AW009','Botanical Study','Watercolor botanical illustrations','PAINTING','2025-02-12','A009'),
('AW010','Indie Track','Indie music recording','MUSIC','2025-02-15','A010'),
('AW011','3D Hero','3D model design','OTHER','2025-02-18','A011'),
('AW012','Light Installation','Installation with LEDs','OTHER','2025-02-20','A012'),
('AW013','Street Mural','Colorful wall mural','PAINTING','2025-02-22','A013'),
('AW014','Violin Sonata','Classical violin piece','MUSIC','2025-02-25','A014'),
('AW015','Portrait Shot','Studio portrait photography','PHOTOGRAPHY','2025-03-01','A015'),
('AW016','Glass Sculpture','Decorative glass','SCULPTURE','2025-03-03','A016'),
('AW017','Modern Abstract','Experimental abstract painting','PAINTING','2025-03-05','A017'),
('AW018','Electronic Mix','Electronic music track','MUSIC','2025-03-07','A018'),
('AW019','Comic Panel','Illustrated comic page','OTHER','2025-03-10','A019'),
('AW020','Admin Sample Art','Sample artwork for admin','OTHER','2025-03-12','A020');

-- ------------------------------
-- Gallery
-- ------------------------------
INSERT INTO Gallery(gallery_id, address, name, rating, admin_id) VALUES
('G001','12 Rue de Rivoli, Paris','Louvre Art House',4.9,'AD001'),
('G002','25 Avenue Victor Hugo, Lyon','Modern Vision Gallery',4.7,'AD002'),
('G003','8 Rue Nationale, Marseille','Mediterranean Art Space',4.6,'AD003'),
('G004','15 Place du Capitole, Toulouse','Southern Gallery',4.5,'AD004'),
('G005','4 Promenade des Anglais, Nice','Blue Coast Gallery',4.8,'AD005'),
('G006','10 Rue Crébillon, Nantes','Creative Hub',4.4,'AD006'),
('G007','18 Grand Rue, Strasbourg','Alsace Art Center',4.7,'AD007'),
('G008','9 Quai des Chartrons, Bordeaux','River Art Gallery',4.6,'AD008'),
('G009','3 Rue Faidherbe, Lille','Northern Art Space',4.5,'AD009'),
('G010','6 Rue Saint-Georges, Rennes','Rennes Art House',4.3,'AD010');

-- ------------------------------
-- Exhibition
-- ------------------------------
INSERT INTO Exhibition(exhibition_id, title, description, event_date, capacity, gallery_id) VALUES
('EX001','Abstract Art Exhibition','Showcasing abstract paintings','2025-03-15',50,'G001'),
('EX002','Watercolor Exhibition','Botanical paintings display','2025-04-20',40,'G002'),
('EX003','3D Art Exhibition','3D digital designs','2025-04-25',30,'G003'),
('EX004','Light Installation Show','LED installation display','2025-04-27',35,'G004'),
('EX005','Photography Exhibit','Portrait photos on display','2025-05-05',25,'G005'),
('EX006','Glass Art Exhibit','Decorative glass display','2025-05-08',20,'G006'),
('EX007','Modern Abstract Show','Experimental paintings exhibition','2025-05-10',50,'G007'),
('EX008','Comic Con Display','Comic panels and artworks','2025-05-15',100,'G008'),
('EX009','Spring Art Fair','Various artworks exhibition','2025-05-20',150,'G009'),
('EX010','Urban Shadows Expo','Street photography exhibition','2025-05-25',60,'G010');

-- ------------------------------
-- Workshop
-- ------------------------------
INSERT INTO Workshop(workshop_id, title, description, workshop_date, location, capacity, price, level, artist_id) VALUES
('W001','Digital Art Workshop','Character drawing workshop','2025-04-05','Community Center',30,49.99,'BEGINNER','A003'),
('W002','Landscape Painting Class','Oil painting lesson','2025-04-12','Studio One',25,59.99,'INTERMEDIATE','A006'),
('W003','Ceramic Workshop','Hands-on pottery workshop','2025-04-18','Art Studio',15,69.99,'BEGINNER','A008'),
('W004','Street Photography Tour','Urban photography event','2025-04-10','City Streets',20,39.99,'BEGINNER','A005'),
('W005','Jazz Improvisation','Learn jazz improvisation basics','2025-04-15','Jazz Club',20,79.99,'ADVANCED','A007'),
('W006','Watercolor Basics','Introduction to watercolor painting','2025-04-22','Gallery Two',30,44.99,'BEGINNER','A009'),
('W007','3D Modeling Basics','Introduction to 3D design','2025-04-28','Digital Lab',18,89.99,'INTERMEDIATE','A011'),
('W008','Violin Masterclass','Classical violin training session','2025-05-02','Concert Hall',12,99.99,'ADVANCED','A014'),
('W009','Comic Drawing Workshop','Create your first comic panel','2025-05-15','Convention Hall',25,54.99,'BEGINNER','A019'),
('W010','Electronic Music Production','Create electronic music tracks','2025-05-12','Club One',20,74.99,'INTERMEDIATE','A018');

-- ------------------------------
-- WorkshopBooking
-- ------------------------------
INSERT INTO WorkshopBooking(booking_id, booking_date, payment_status, member_id, workshop_id) VALUES
('B001','2025-03-01 10:00:00','PAID','M001','W001'),
('B002','2025-03-05 11:30:00','PAID','M001','W002'),
('B003','2025-03-08 09:15:00','PENDING','M002','W001'),
('B004','2025-03-10 14:00:00','PAID','M003','W003'),
('B005','2025-03-12 16:45:00','PAID','M004','W004'),
('B006','2025-03-15 12:00:00','PENDING','M005','W005'),
('B007','2025-03-17 13:30:00','PAID','M006','W006'),
('B008','2025-03-20 15:00:00','PAID','M007','W007'),
('B009','2025-03-22 17:20:00','PAID','M008','W008'),
('B010','2025-03-25 10:10:00','PAID','M009','W009'),
('B011','2025-03-27 11:50:00','PAID','M010','W010'),
('B012','2025-03-30 09:00:00','PENDING','M011','W001'),
('B013','2025-04-02 10:30:00','PAID','M012','W002'),
('B014','2025-04-05 14:15:00','PAID','M013','W003'),
('B015','2025-04-07 16:00:00','PAID','M014','W004'),
('B016','2025-04-10 13:45:00','PAID','M015','W005'),
('B017','2025-04-12 15:20:00','PAID','M016','W006'),
('B018','2025-04-15 18:00:00','PENDING','M017','W007'),
('B019','2025-04-18 12:40:00','PAID','M018','W008'),
('B020','2025-04-20 10:25:00','PAID','M019','W009');

-- ------------------------------
-- Follows
-- ------------------------------
INSERT INTO Follows(member_id, artist_id) VALUES
('M001','A001'),
('M001','A002'),
('M002','A001'),
('M002','A003'),
('M003','A002'),
('M003','A004'),
('M004','A005'),
('M004','A006'),
('M005','A003'),
('M005','A007'),
('M006','A004'),
('M006','A008'),
('M007','A009'),
('M007','A010'),
('M008','A001'),
('M008','A002'),
('M009','A003'),
('M009','A004'),
('M010','A005'),
('M010','A006');

-- ------------------------------
-- presented_in
-- ------------------------------
INSERT INTO presented_in(artwork_id, exhibition_id) VALUES
('AW001','EX001'),
('AW002','EX001'),
('AW009','EX002'),
('AW011','EX003'),
('AW012','EX004'),
('AW015','EX005'),
('AW016','EX006'),
('AW017','EX007'),
('AW019','EX008'),
('AW001','EX009'),
('AW003','EX009'),
('AW005','EX010'),
('AW006','EX009'),
('AW008','EX006'),
('AW013','EX007');

-- ------------------------------
-- Views
-- ------------------------------

CREATE VIEW vw_artist_summary AS
SELECT
    a.artist_id,
    u.name,
    a.discipline,
    a.birth_year,
    a.city
FROM Artist a
JOIN UserAccount u ON a.user_id = u.user_id;

CREATE VIEW vw_exhibition_summary AS
SELECT
    e.exhibition_id,
    e.title,
    e.event_date,
    e.capacity,
    g.name AS gallery_name
FROM Exhibition e
JOIN Gallery g ON e.gallery_id = g.gallery_id;

CREATE VIEW vw_users_sanitized AS
SELECT
    user_id,
    name,
    email,
    role
FROM UserAccount;

CREATE VIEW vw_member_workshop_bookings AS
SELECT
    m.member_id,
    u.name AS member_name,
    w.workshop_id,
    w.title AS workshop_title,
    wb.booking_date,
    wb.payment_status
FROM CommunityMember m
JOIN UserAccount u ON m.user_id = u.user_id
JOIN WorkshopBooking wb ON m.member_id = wb.member_id
JOIN Workshop w ON wb.workshop_id = w.workshop_id;

CREATE VIEW vw_artist_followers AS
SELECT
    f.artist_id,
    a.biography,
    f.member_id,
    u.name AS member_name
FROM Follows f
JOIN Artist a ON f.artist_id = a.artist_id
JOIN CommunityMember m ON f.member_id = m.member_id
JOIN UserAccount u ON m.user_id = u.user_id;

CREATE VIEW vw_artworks_in_exhibitions AS
SELECT
    aw.artwork_id,
    aw.title AS artwork_title,
    ex.exhibition_id,
    ex.title AS exhibition_title,
    ex.event_date
FROM presented_in pi
JOIN Artwork aw ON pi.artwork_id = aw.artwork_id
JOIN Exhibition ex ON pi.exhibition_id = ex.exhibition_id;

-- ------------------------------
-- Indexes
-- ------------------------------

CREATE INDEX idx_artist_discipline ON Artist(discipline);

CREATE INDEX idx_artist_city ON Artist(city);

CREATE INDEX idx_artwork_artist ON Artwork(artist_id);

CREATE INDEX idx_gallery_admin ON Gallery(admin_id);

CREATE INDEX idx_exhibition_date ON Exhibition(event_date);

CREATE INDEX idx_exhibition_gallery ON Exhibition(gallery_id);

CREATE INDEX idx_workshop_artist ON Workshop(artist_id);

CREATE INDEX idx_workshop_date ON Workshop(workshop_date);

CREATE INDEX idx_booking_workshop_member ON WorkshopBooking(workshop_id, member_id);

CREATE INDEX idx_follow_member_artist ON Follows(member_id, artist_id);

CREATE INDEX idx_presented_exhibition ON presented_in(exhibition_id, artwork_id);

-- ------------------------------
-- Triggers
-- ------------------------------

DELIMITER //

CREATE TRIGGER trg_check_workshop_capacity
BEFORE INSERT ON Workshop
FOR EACH ROW
BEGIN
    IF NEW.capacity <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Workshop capacity must be positive';
    END IF;
END;
//

DELIMITER ;

DELIMITER //

CREATE TRIGGER trg_workshop_capacity_check
BEFORE INSERT ON WorkshopBooking
FOR EACH ROW
BEGIN
    DECLARE cap INT;

    SELECT capacity INTO cap
    FROM Workshop
    WHERE workshop_id = NEW.workshop_id;

    IF cap <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Workshop is full';
    END IF;
END;
//

DELIMITER ;

DELIMITER //

CREATE TRIGGER trg_update_workshop_capacity
AFTER INSERT ON WorkshopBooking
FOR EACH ROW
BEGIN
    UPDATE Workshop
    SET capacity = capacity - 1
    WHERE workshop_id = NEW.workshop_id
      AND capacity > 0;
END;
//

DELIMITER ;

DELIMITER //

CREATE TRIGGER trg_prevent_duplicate_follow
BEFORE INSERT ON Follows
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
        FROM Follows
        WHERE member_id = NEW.member_id
          AND artist_id = NEW.artist_id
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Member already follows this artist';
    END IF;
END;
//

DELIMITER ;

-- ------------------------------
-- Procedures
-- ------------------------------

DELIMITER //

CREATE PROCEDURE sp_create_workshop(
    IN p_workshop_id VARCHAR(50),
    IN p_title VARCHAR(100),
    IN p_description TEXT,
    IN p_workshop_date DATE,
    IN p_location VARCHAR(100),
    IN p_capacity INT,
    IN p_price DECIMAL(10,2),
    IN p_level VARCHAR(100),
    IN p_artist_id VARCHAR(50)
)
BEGIN
    IF p_capacity <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Capacity must be positive';
    END IF;

    INSERT INTO Workshop(
        workshop_id,
        title,
        description,
        workshop_date,
        location,
        capacity,
        price,
        level,
        artist_id
    )
    VALUES(
        p_workshop_id,
        p_title,
        p_description,
        p_workshop_date,
        p_location,
        p_capacity,
        p_price,
        p_level,
        p_artist_id
    );
END;
//

DELIMITER ;

DELIMITER //

CREATE PROCEDURE sp_book_workshop(
    IN p_booking_id VARCHAR(50),
    IN p_member_id VARCHAR(50),
    IN p_workshop_id VARCHAR(50)
)
BEGIN
    DECLARE remaining_capacity INT;

    SELECT capacity INTO remaining_capacity
    FROM Workshop
    WHERE workshop_id = p_workshop_id;

    IF remaining_capacity <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Workshop is full';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM WorkshopBooking
        WHERE member_id = p_member_id
          AND workshop_id = p_workshop_id
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Member already booked this workshop';
    END IF;

    INSERT INTO WorkshopBooking(
        booking_id,
        booking_date,
        payment_status,
        member_id,
        workshop_id
    )
    VALUES(
        p_booking_id,
        NOW(),
        'PAID',
        p_member_id,
        p_workshop_id
    );
END;
//

DELIMITER ;

DELIMITER //

CREATE PROCEDURE sp_follow_artist(
    IN p_member_id VARCHAR(50),
    IN p_artist_id VARCHAR(50)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM Follows
        WHERE member_id = p_member_id
          AND artist_id = p_artist_id
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Member already follows this artist';
    END IF;

    INSERT INTO Follows(member_id, artist_id)
    VALUES(p_member_id, p_artist_id);
END;
//

DELIMITER ;

DELIMITER //

CREATE PROCEDURE sp_cancel_workshop_booking(
    IN p_booking_id VARCHAR(50)
)
BEGIN
    DECLARE v_workshop_id VARCHAR(50);

    SELECT workshop_id INTO v_workshop_id
    FROM WorkshopBooking
    WHERE booking_id = p_booking_id;

    DELETE FROM WorkshopBooking
    WHERE booking_id = p_booking_id;

    UPDATE Workshop
    SET capacity = capacity + 1
    WHERE workshop_id = v_workshop_id;
END;
//

DELIMITER ;

-- ------------------------------
-- Function
-- ------------------------------

DELIMITER //

CREATE FUNCTION fn_workshop_booking_count(p_workshop_id VARCHAR(50))
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE cnt INT;

    SELECT COUNT(*) INTO cnt
    FROM WorkshopBooking
    WHERE workshop_id = p_workshop_id
      AND payment_status = 'PAID';

    RETURN cnt;
END;
//

DELIMITER ;


START TRANSACTION;

CALL sp_book_workshop('B021','M020','W001');
CALL sp_book_workshop('B022','M020','W002');
CALL sp_book_workshop('B023','M020','W003');

COMMIT;


