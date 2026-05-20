package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC implementation for ArtistDao.
 */
public class JdbcArtistDao implements ArtistDao {

    @Override
    public List<Artist> findAll() {
        ensureArtistProfileColumns();
        List<Artist> artists = new ArrayList<>();

        String sql = """
                SELECT
                    a.artist_id,
                    a.biography,
                    a.discipline,
                    a.birth_year,
                    a.city,
                    u.user_id,
                    u.name,
                    u.email
                FROM Artist a
                JOIN UserAccount u ON a.user_id = u.user_id
                ORDER BY u.name
                """;

        try (
                Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                artists.add(mapArtist(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while loading artists from database.", e);
        }

        return artists;
    }

    @Override
    public void save(Artist artist) {
        ensureArtistProfileColumns();
        String userId = generateId("U");
        String artistId = generateId("A");

        String insertUser = """
                INSERT INTO UserAccount(user_id, email, name, password, role)
                VALUES (?, ?, ?, ?, 'ARTIST')
                """;

        String insertArtist = """
                INSERT INTO Artist(artist_id, biography, discipline, birth_year, city, user_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);

            try (
                    PreparedStatement userStatement = connection.prepareStatement(insertUser);
                    PreparedStatement artistStatement = connection.prepareStatement(insertArtist)
            ) {
                userStatement.setString(1, userId);
                userStatement.setString(2, safeEmail(artist.getContactEmail(), userId));
                userStatement.setString(3, artist.getName());
                userStatement.setString(4, "defaultPassword");
                userStatement.executeUpdate();

                artistStatement.setString(1, artistId);
                artistStatement.setString(2, safeText(artist.getBio(), ""));
                artistStatement.setString(3, getFirstDiscipline(artist));
                artistStatement.setInt(4, safeBirthYear(artist.getBirthYear()));
                artistStatement.setString(5, safeText(artist.getCity(), "Unknown"));
                artistStatement.setString(6, userId);
                artistStatement.executeUpdate();

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error while saving artist into database.", e);
        }
    }

    @Override
    public void update(Artist artist) {
        ensureArtistProfileColumns();
        String selectIds = """
                SELECT a.artist_id, u.user_id
                FROM Artist a
                JOIN vw_users_sanitized u ON a.user_id = u.user_id
                WHERE u.name = ?
                """;

        String updateUser = """
                UPDATE UserAccount
                SET email = ?, name = ?
                WHERE user_id = ?
                """;

        String updateArtist = """
                UPDATE Artist
                SET biography = ?, discipline = ?, birth_year = ?, city = ?
                WHERE artist_id = ?
                """;

        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);

            try (
                    PreparedStatement selectStatement = connection.prepareStatement(selectIds);
                    PreparedStatement userStatement = connection.prepareStatement(updateUser);
                    PreparedStatement artistStatement = connection.prepareStatement(updateArtist)
            ) {
                selectStatement.setString(1, artist.getName());

                String artistId;
                String userId;

                try (ResultSet resultSet = selectStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Artist not found: " + artist.getName());
                    }

                    artistId = resultSet.getString("artist_id");
                    userId = resultSet.getString("user_id");
                }

                userStatement.setString(1, safeEmail(artist.getContactEmail(), userId));
                userStatement.setString(2, artist.getName());
                userStatement.setString(3, userId);
                userStatement.executeUpdate();

                artistStatement.setString(1, safeText(artist.getBio(), ""));
                artistStatement.setString(2, getFirstDiscipline(artist));
                artistStatement.setInt(3, safeBirthYear(artist.getBirthYear()));
                artistStatement.setString(4, safeText(artist.getCity(), "Unknown"));
                artistStatement.setString(5, artistId);
                artistStatement.executeUpdate();

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error while updating artist in database.", e);
        }
    }

    @Override
    public void delete(String artistName) {
        ensureArtistProfileColumns();
        String findArtist = """
                SELECT a.artist_id, u.user_id
                FROM Artist a
                JOIN vw_users_sanitized u ON a.user_id = u.user_id
                WHERE u.name = ?
                """;

        String deletePresentedIn = """
                DELETE pi FROM presented_in pi
                JOIN Artwork aw ON pi.artwork_id = aw.artwork_id
                WHERE aw.artist_id = ?
                """;
        String deleteArtworks = "DELETE FROM Artwork WHERE artist_id = ?";
        String deleteFollows = "DELETE FROM Follows WHERE artist_id = ?";
        String deleteWorkshopBookings = """
                DELETE wb FROM WorkshopBooking wb
                JOIN Workshop w ON wb.workshop_id = w.workshop_id
                WHERE w.artist_id = ?
                """;
        String deleteWorkshops = "DELETE FROM Workshop WHERE artist_id = ?";
        String deleteArtist = "DELETE FROM Artist WHERE artist_id = ?";
        String deleteUser = "DELETE FROM UserAccount WHERE user_id = ?";

        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement findStatement = connection.prepareStatement(findArtist)) {
                findStatement.setString(1, artistName);

                String artistId;
                String userId;

                try (ResultSet resultSet = findStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        return;
                    }

                    artistId = resultSet.getString("artist_id");
                    userId = resultSet.getString("user_id");
                }

                executeDeleteIfTableExists(connection, "presented_in", deletePresentedIn, artistId);
                executeDelete(connection, deleteArtworks, artistId);
                executeDelete(connection, deleteFollows, artistId);
                executeDeleteIfTableExists(connection, "WorkshopBooking", deleteWorkshopBookings, artistId);
                executeDeleteIfTableExists(connection, "Workshop", deleteWorkshops, artistId);
                executeDelete(connection, deleteArtist, artistId);
                executeDelete(connection, deleteUser, userId);

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error while deleting artist from database.", e);
        }
    }

    @Override
    public List<Artist> findByCity(String city) {
        ensureArtistProfileColumns();
        List<Artist> artists = new ArrayList<>();

        String sql = """
                SELECT
                    a.artist_id,
                    a.biography,
                    a.discipline,
                    a.birth_year,
                    a.city,
                    u.user_id,
                    u.name,
                    u.email
                FROM Artist a
                JOIN UserAccount u ON a.user_id = u.user_id
                WHERE LOWER(a.city) = LOWER(?)
                ORDER BY u.name
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, city);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    artists.add(mapArtist(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while loading artists by city.", e);
        }

        return artists;
    }

    private Artist mapArtist(ResultSet resultSet) throws SQLException {
        Artist artist = new Artist();

        artist.setName(resultSet.getString("name"));
        artist.setBio(resultSet.getString("biography"));
        artist.setContactEmail(resultSet.getString("email"));
        artist.setBirthYear(getNullableInt(resultSet, "birth_year"));
        artist.setCity(resultSet.getString("city"));
        artist.setActive(true);

        addDisciplineIfPossible(artist, resultSet.getString("discipline"));

        return artist;
    }

    private void addDisciplineIfPossible(Artist artist, String disciplineValue) {
        if (disciplineValue == null || disciplineValue.isBlank()) {
            return;
        }

        artist.getDisciplines().add(new Discipline(disciplineValue));
    }
    private String getFirstDiscipline(Artist artist) {
        if (artist.getDisciplines() == null || artist.getDisciplines().isEmpty()) {
            return "OTHER";
        }

        String disciplineName = artist.getDisciplines().get(0).getName();

        if (disciplineName == null || disciplineName.isBlank()) {
            return "OTHER";
        }

        return disciplineName.toUpperCase();
    }
    private String safeEmail(String email, String userId) {
        if (email == null || email.isBlank()) {
            return userId.toLowerCase() + "@artconnect.local";
        }

        return email;
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Integer getNullableInt(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private int safeBirthYear(Integer value) {
        return value == null ? 0 : value;
    }

    private void ensureArtistProfileColumns() {
        try (Connection connection = ConnectionManager.getConnection()) {
            ensureColumnExists(connection, "birth_year", "INT NULL");
            ensureColumnExists(connection, "city", "VARCHAR(100) NULL");
            executeUpdate(connection, "UPDATE Artist SET biography = '' WHERE biography IS NULL");
            executeUpdate(connection, "UPDATE Artist SET discipline = 'OTHER' WHERE discipline IS NULL OR discipline = ''");
            executeUpdate(connection, "UPDATE Artist SET birth_year = 0 WHERE birth_year IS NULL");
            executeUpdate(connection, "UPDATE Artist SET city = 'Unknown' WHERE city IS NULL OR city = ''");
            executeUpdate(connection, "ALTER TABLE Artist MODIFY biography TEXT NOT NULL");
            executeUpdate(connection, "ALTER TABLE Artist MODIFY discipline VARCHAR(50) NOT NULL");
            executeUpdate(connection, "ALTER TABLE Artist MODIFY birth_year INT NOT NULL");
            executeUpdate(connection, "ALTER TABLE Artist MODIFY city VARCHAR(100) NOT NULL");
        } catch (SQLException e) {
            throw new RuntimeException("Error while ensuring Artist profile columns exist.", e);
        }
    }

    private void executeUpdate(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private void ensureColumnExists(Connection connection, String columnName, String definition) throws SQLException {
        String existsSql = """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'Artist'
                  AND COLUMN_NAME = ?
                """;

        try (PreparedStatement existsStatement = connection.prepareStatement(existsSql)) {
            existsStatement.setString(1, columnName);

            try (ResultSet resultSet = existsStatement.executeQuery()) {
                if (resultSet.next() && resultSet.getInt(1) > 0) {
                    return;
                }
            }
        }

        try (PreparedStatement alterStatement = connection.prepareStatement(
                "ALTER TABLE Artist ADD COLUMN " + columnName + " " + definition
        )) {
            alterStatement.executeUpdate();
        }
    }

    private String generateId(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void executeDelete(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            statement.executeUpdate();
        }
    }

    private void executeDeleteIfTableExists(Connection connection, String tableName, String sql, String value) throws SQLException {
        if (!tableExists(connection, tableName)) {
            return;
        }

        executeDelete(connection, sql, value);
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
}
