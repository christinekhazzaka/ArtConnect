package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC implementation for ArtworkDao.
 */
public class JdbcArtworkDao implements ArtworkDao {

    @Override
    public List<Artwork> findAll() {
        ensureFinalSchema();
        List<Artwork> artworks = new ArrayList<>();

        String sql = """
                SELECT
                    aw.artwork_id,
                    aw.title,
                    aw.description,
                    aw.type,
                    aw.creation_date,
                    ar.artist_id,
                    ar.biography,
                    ar.discipline,
                    u.name AS artist_name,
                    u.email AS artist_email
                FROM Artwork aw
                JOIN Artist ar ON aw.artist_id = ar.artist_id
                JOIN vw_users_sanitized u ON ar.user_id = u.user_id
                ORDER BY aw.title
                """;

        try (
                Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                artworks.add(mapArtwork(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while loading artworks from database.", e);
        }

        return artworks;
    }

    @Override
    public void save(Artwork artwork) {
        ensureFinalSchema();
        String artistId = findArtistIdByName(artwork.getArtist().getName());

        String sql = """
                INSERT INTO Artwork(artwork_id, title, description, type, creation_date, artist_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (
                Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, generateId("AW"));
            statement.setString(2, artwork.getTitle());
            statement.setString(3, safeText(artwork.getDescription(), ""));
            statement.setString(4, normalizeArtworkType(artwork.getType()));
            statement.setDate(5, toSqlDate(artwork.getCreationYear()));
            statement.setString(6, artistId);

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error while saving artwork into database.", e);
        }
    }

    @Override
    public void update(Artwork artwork) {
        ensureFinalSchema();
        String sql = """
                UPDATE Artwork
                SET description = ?,
                    type = ?,
                    creation_date = ?
                WHERE title = ?
                """;

        try (
                Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, safeText(artwork.getDescription(), ""));
            statement.setString(2, normalizeArtworkType(artwork.getType()));
            statement.setDate(3, toSqlDate(artwork.getCreationYear()));
            statement.setString(4, artwork.getTitle());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error while updating artwork in database.", e);
        }
    }

    @Override
    public void delete(String title) {
        ensureFinalSchema();
        String findSql = "SELECT artwork_id FROM Artwork WHERE title = ?";
        String deleteLinks = "DELETE FROM presented_in WHERE artwork_id = ?";
        String deleteArtwork = "DELETE FROM Artwork WHERE title = ?";

        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            boolean presentedInExists = tableExists(connection, "presented_in");
            try (PreparedStatement findStatement = connection.prepareStatement(findSql);
                 PreparedStatement deleteArtworkStatement = connection.prepareStatement(deleteArtwork)) {
                findStatement.setString(1, title);
                try (ResultSet resultSet = findStatement.executeQuery()) {
                    while (resultSet.next()) {
                        if (presentedInExists) {
                            try (PreparedStatement deleteLinksStatement = connection.prepareStatement(deleteLinks)) {
                                deleteLinksStatement.setString(1, resultSet.getString("artwork_id"));
                                deleteLinksStatement.executeUpdate();
                            }
                        }
                    }
                }

                deleteArtworkStatement.setString(1, title);
                deleteArtworkStatement.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while deleting artwork from database.", e);
        }
    }

    @Override
    public List<Artwork> findByArtistName(String artistName) {
        ensureFinalSchema();
        List<Artwork> artworks = new ArrayList<>();

        String sql = """
                SELECT
                    aw.artwork_id,
                    aw.title,
                    aw.description,
                    aw.type,
                    aw.creation_date,
                    ar.artist_id,
                    ar.biography,
                    ar.discipline,
                    u.name AS artist_name,
                    u.email AS artist_email
                FROM Artwork aw
                JOIN Artist ar ON aw.artist_id = ar.artist_id
                JOIN vw_users_sanitized u ON ar.user_id = u.user_id
                WHERE u.name = ?
                ORDER BY aw.title
                """;

        try (
                Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, artistName);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    artworks.add(mapArtwork(resultSet));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error while loading artworks by artist name.", e);
        }

        return artworks;
    }

    private Artwork mapArtwork(ResultSet resultSet) throws SQLException {
        Artist artist = new Artist();
        artist.setName(resultSet.getString("artist_name"));
        artist.setBio(resultSet.getString("biography"));
        artist.setContactEmail(resultSet.getString("artist_email"));
        artist.setActive(true);

        Artwork artwork = new Artwork();
        artwork.setTitle(resultSet.getString("title"));
        artwork.setDescription(resultSet.getString("description"));
        artwork.setType(resultSet.getString("type"));
        artwork.setCreationYear(extractYear(resultSet.getDate("creation_date")));
        artwork.setPrice(0.0);
        artwork.setStatus(Artwork.Status.FOR_SALE);
        artwork.setArtist(artist);

        return artwork;
    }

    private String findArtistIdByName(String artistName) {
        String sql = """
                SELECT artist_id
                FROM vw_artist_summary
                WHERE name = ?
                """;

        try (
                Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, artistName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("artist_id");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error while finding artist id.", e);
        }

        throw new RuntimeException("Artist not found: " + artistName);
    }

    private Date toSqlDate(Integer creationYear) {
        int year = creationYear != null ? creationYear : LocalDate.now().getYear();
        return Date.valueOf(LocalDate.of(year, 1, 1));
    }

    private Integer extractYear(Date date) {
        if (date == null) {
            return null;
        }

        return date.toLocalDate().getYear();
    }

    private String normalizeArtworkType(String type) {
        if (type == null || type.isBlank()) {
            return "OTHER";
        }

        String normalized = type.trim().toUpperCase();

        return switch (normalized) {
            case "PAINTING", "PHOTOGRAPHY", "SCULPTURE", "MUSIC", "OTHER" -> normalized;
            default -> "OTHER";
        };
    }

    private void ensureFinalSchema() {
        try (Connection connection = ConnectionManager.getConnection()) {
            executeUpdate(connection, "UPDATE Artwork SET description = '' WHERE description IS NULL");
            executeUpdate(connection, "ALTER TABLE Artwork MODIFY title VARCHAR(50) NOT NULL");
            executeUpdate(connection, "ALTER TABLE Artwork MODIFY description TEXT NOT NULL");
            executeUpdate(connection, "ALTER TABLE Artwork MODIFY type VARCHAR(50) NOT NULL");
            executeUpdate(connection, "ALTER TABLE Artwork MODIFY creation_date DATE NOT NULL");
            addUniqueTitleIfMissing(connection);
        } catch (SQLException e) {
            throw new RuntimeException("Error while ensuring Artwork table matches the final schema.", e);
        }
    }

    private void addUniqueTitleIfMissing(Connection connection) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'Artwork'
                  AND COLUMN_NAME = 'title'
                  AND NON_UNIQUE = 0
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next() && resultSet.getInt(1) > 0) {
                return;
            }
        }

        executeUpdate(connection, "ALTER TABLE Artwork ADD CONSTRAINT artwork_title_unique UNIQUE (title)");
    }

    private void executeUpdate(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String generateId(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
