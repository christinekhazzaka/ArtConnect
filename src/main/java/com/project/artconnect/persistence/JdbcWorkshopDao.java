package com.project.artconnect.persistence;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcWorkshopDao implements WorkshopDao {

    @Override
    public Optional<Workshop> findByTitle(String title) {
        ensureTableExists();

        String sql = baseSelect() + " WHERE w.title = ?";

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapWorkshop(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while finding workshop by title.", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Workshop> findAll() {
        ensureTableExists();

        List<Workshop> workshops = new ArrayList<>();
        String sql = baseSelect() + " ORDER BY w.workshop_date, w.title";

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                workshops.add(mapWorkshop(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while loading workshops.", e);
        }

        return workshops;
    }

    @Override
    public void save(Workshop workshop) {
        ensureTableExists();

        String sql = "{CALL sp_create_workshop(?, ?, ?, ?, ?, ?, ?, ?, ?)}";

        try (Connection connection = ConnectionManager.getConnection();
             CallableStatement statement = connection.prepareCall(sql)) {
            statement.setString(1, generateId("W"));
            statement.setString(2, workshop.getTitle());
            statement.setString(3, safeText(workshop.getDescription(), ""));
            statement.setDate(4, Date.valueOf(toLocalDate(workshop.getDate())));
            statement.setString(5, safeText(workshop.getLocation(), "Workshop"));
            statement.setInt(6, Math.max(1, workshop.getMaxParticipants()));
            statement.setDouble(7, workshop.getPrice());
            statement.setString(8, normalizeLevel(workshop.getLevel()));
            statement.setString(9, findArtistIdByName(workshop.getInstructor().getName()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error while saving workshop with stored procedure.", e);
        }
    }

    @Override
    public void update(Workshop workshop) {
        ensureTableExists();

        String sql = """
                UPDATE Workshop
                SET description = ?, workshop_date = ?, location = ?, capacity = ?, price = ?, level = ?, artist_id = ?
                WHERE title = ?
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, safeText(workshop.getDescription(), ""));
            statement.setDate(2, Date.valueOf(toLocalDate(workshop.getDate())));
            statement.setString(3, safeText(workshop.getLocation(), "Workshop"));
            statement.setInt(4, Math.max(1, workshop.getMaxParticipants()));
            statement.setDouble(5, workshop.getPrice());
            statement.setString(6, normalizeLevel(workshop.getLevel()));
            statement.setString(7, findArtistIdByName(workshop.getInstructor().getName()));
            statement.setString(8, workshop.getTitle());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error while updating workshop.", e);
        }
    }

    @Override
    public void delete(String title) {
        ensureTableExists();

        String findSql = "SELECT workshop_id FROM Workshop WHERE title = ?";
        String deleteBookingsSql = "DELETE FROM WorkshopBooking WHERE workshop_id = ?";
        String deleteWorkshopSql = "DELETE FROM Workshop WHERE title = ?";

        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement findStatement = connection.prepareStatement(findSql);
                 PreparedStatement deleteWorkshopStatement = connection.prepareStatement(deleteWorkshopSql)) {
                findStatement.setString(1, title);
                try (ResultSet resultSet = findStatement.executeQuery()) {
                    while (resultSet.next() && tableExists(connection, "WorkshopBooking")) {
                        try (PreparedStatement deleteBookingsStatement = connection.prepareStatement(deleteBookingsSql)) {
                            deleteBookingsStatement.setString(1, resultSet.getString("workshop_id"));
                            deleteBookingsStatement.executeUpdate();
                        }
                    }
                }

                deleteWorkshopStatement.setString(1, title);
                deleteWorkshopStatement.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while deleting workshop.", e);
        }
    }

    private String baseSelect() {
        return """
                SELECT w.workshop_id, w.title, w.description, w.workshop_date, w.location, w.capacity,
                       w.price, w.level,
                       a.artist_id, a.biography, a.discipline, u.name AS artist_name, u.email AS artist_email
                FROM Workshop w
                JOIN Artist a ON w.artist_id = a.artist_id
                JOIN UserAccount u ON a.user_id = u.user_id
                """;
    }

    private Workshop mapWorkshop(ResultSet resultSet) throws SQLException {
        Artist instructor = new Artist();
        instructor.setName(resultSet.getString("artist_name"));
        instructor.setBio(resultSet.getString("biography"));
        instructor.setContactEmail(resultSet.getString("artist_email"));

        Workshop workshop = new Workshop(
                resultSet.getString("title"),
                toLocalDateTime(resultSet.getDate("workshop_date")),
                instructor,
                resultSet.getDouble("price")
        );
        workshop.setDescription(resultSet.getString("description"));
        workshop.setLocation(resultSet.getString("location"));
        workshop.setMaxParticipants(resultSet.getInt("capacity"));
        workshop.setDurationMinutes(180);
        workshop.setLevel(resultSet.getString("level"));
        return workshop;
    }

    private void ensureTableExists() {
        String createSql = """
                CREATE TABLE IF NOT EXISTS Workshop (
                    workshop_id VARCHAR(50) NOT NULL PRIMARY KEY,
                    title VARCHAR(100) NOT NULL UNIQUE,
                    description TEXT NOT NULL,
                    workshop_date DATE NOT NULL,
                    location VARCHAR(100) NOT NULL,
                    capacity INT NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    level VARCHAR(100) NOT NULL,
                    artist_id VARCHAR(50) NOT NULL,
                    KEY idx_workshop_artist (artist_id),
                    KEY idx_workshop_date (workshop_date),
                    CONSTRAINT workshop_artist_fk FOREIGN KEY (artist_id) REFERENCES Artist(artist_id),
                    CONSTRAINT chk_workshop_capacity CHECK (capacity > 0)
                )
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(createSql)) {
            statement.executeUpdate();
            alignFinalColumns(connection);
        } catch (SQLException e) {
            throw new RuntimeException("Error while ensuring Workshop table exists.", e);
        }
    }

    private void alignFinalColumns(Connection connection) throws SQLException {
        executeUpdate(connection, "UPDATE Workshop SET description = '' WHERE description IS NULL");
        executeUpdate(connection, "UPDATE Workshop SET location = 'Workshop' WHERE location IS NULL OR location = ''");
        executeUpdate(connection, "UPDATE Workshop SET capacity = 1 WHERE capacity IS NULL OR capacity < 1");
        executeUpdate(connection, "UPDATE Workshop SET price = 0 WHERE price IS NULL");
        executeUpdate(connection, "UPDATE Workshop SET level = 'BEGINNER' WHERE level IS NULL OR level = ''");
        executeUpdate(connection, "UPDATE Workshop SET level = UPPER(level)");
        executeUpdate(connection, "ALTER TABLE Workshop MODIFY title VARCHAR(100) NOT NULL");
        executeUpdate(connection, "ALTER TABLE Workshop MODIFY description TEXT NOT NULL");
        executeUpdate(connection, "ALTER TABLE Workshop MODIFY workshop_date DATE NOT NULL");
        executeUpdate(connection, "ALTER TABLE Workshop MODIFY location VARCHAR(100) NOT NULL");
        executeUpdate(connection, "ALTER TABLE Workshop MODIFY capacity INT NOT NULL");
        executeUpdate(connection, "ALTER TABLE Workshop MODIFY price DECIMAL(10,2) NOT NULL");
        executeUpdate(connection, "ALTER TABLE Workshop MODIFY level VARCHAR(100) NOT NULL");
        executeUpdate(connection, "ALTER TABLE Workshop MODIFY artist_id VARCHAR(50) NOT NULL");
    }

    private void executeUpdate(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate() : LocalDate.now();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date != null ? date.toLocalDate().atStartOfDay() : null;
    }

    private String findArtistIdByName(String artistName) throws SQLException {
        String sql = """
                SELECT artist_id
                FROM vw_artist_summary
                WHERE name = ?
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, artistName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("artist_id");
                }
            }
        }

        throw new SQLException("Artist not found: " + artistName);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            return "BEGINNER";
        }

        String normalized = level.trim().toUpperCase();
        return switch (normalized) {
            case "BEGINNER", "INTERMEDIATE", "ADVANCED" -> normalized;
            default -> throw new IllegalArgumentException("Workshop level must be BEGINNER, INTERMEDIATE, or ADVANCED.");
        };
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

    private String generateId(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
