package com.project.artconnect.persistence;

import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcGalleryDao implements GalleryDao {

    @Override
    public List<Gallery> findAll() {
        ensureTableExists();
        List<Gallery> galleries = new ArrayList<>();
        String sql = """
                SELECT gallery_id, name, address, rating
                FROM Gallery
                ORDER BY name
                """;
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                galleries.add(mapGallery(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while loading galleries.", e);
        }
        return galleries;
    }

    @Override
    public Optional<Gallery> findByName(String name) {
        ensureTableExists();
        String sql = """
                SELECT gallery_id, name, address, rating
                FROM Gallery
                WHERE name = ?
                """;
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapGallery(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while finding gallery by name.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Gallery> searchByName(String query) {
        ensureTableExists();
        List<Gallery> galleries = new ArrayList<>();
        String sql = """
                SELECT gallery_id, name, address, rating
                FROM Gallery
                WHERE LOWER(name) LIKE LOWER(?)
                ORDER BY name
                """;
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + query + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    galleries.add(mapGallery(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while searching galleries.", e);
        }
        return galleries;
    }

    @Override
    public void save(Gallery gallery) {
        ensureTableExists();
        String sql = """
                INSERT INTO Gallery (gallery_id, name, address, rating, admin_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, generateId());
            statement.setString(2, safeText(gallery.getName(), "Gallery"));
            statement.setString(3, safeText(gallery.getAddress(), "Gallery"));
            statement.setDouble(4, gallery.getRating());
            statement.setString(5, findAdminId(connection));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error while saving gallery.", e);
        }
    }

    @Override
    public void update(Gallery gallery) {
        ensureTableExists();
        String sql = """
                UPDATE Gallery
                SET address = ?, rating = ?
                WHERE name = ?
                """;
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, safeText(gallery.getAddress(), "Gallery"));
            statement.setDouble(2, gallery.getRating());
            statement.setString(3, gallery.getName());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error while updating gallery.", e);
        }
    }

    @Override
    public void delete(String galleryName) {
        ensureTableExists();
        String sql = """
                DELETE FROM Gallery
                WHERE name = ?
                """;
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, galleryName);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error while deleting gallery.", e);
        }
    }

    private Gallery mapGallery(ResultSet resultSet) throws SQLException {
        String name = resultSet.getString("name");
        String address = resultSet.getString("address");
        double rating = resultSet.getDouble("rating");
        return new Gallery(name, address, rating);
    }

    private void ensureTableExists() {
        String sql = """
                CREATE TABLE IF NOT EXISTS Gallery (
                    gallery_id VARCHAR(50) NOT NULL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL UNIQUE,
                    address VARCHAR(100) NOT NULL,
                    rating DOUBLE NOT NULL,
                    admin_id VARCHAR(50) NOT NULL,
                    KEY idx_gallery_admin (admin_id),
                    CONSTRAINT gallery_admin_fk FOREIGN KEY (admin_id) REFERENCES Administrator(admin_id)
                )
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
            ensureAdminColumn(connection);
            seedFromContentLocationsIfEmpty(connection);
        } catch (SQLException e) {
            throw new RuntimeException("Error while ensuring Gallery table exists.", e);
        }
    }

    private void ensureAdminColumn(Connection connection) throws SQLException {
        if (!columnExists(connection, "Gallery", "admin_id")) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "ALTER TABLE Gallery ADD COLUMN admin_id VARCHAR(50) NULL"
            )) {
                statement.executeUpdate();
            }
        }

        String adminId = findAdminId(connection);
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE Gallery SET admin_id = ? WHERE admin_id IS NULL OR admin_id = ''"
        )) {
            statement.setString(1, adminId);
            statement.executeUpdate();
        }

        executeIgnoringDuplicate(connection, "ALTER TABLE Gallery MODIFY name VARCHAR(100) NOT NULL");
        executeIgnoringDuplicate(connection, "ALTER TABLE Gallery MODIFY address VARCHAR(100) NOT NULL");
        executeIgnoringDuplicate(connection, "ALTER TABLE Gallery MODIFY rating DOUBLE NOT NULL");
        executeIgnoringDuplicate(connection, "ALTER TABLE Gallery MODIFY admin_id VARCHAR(50) NOT NULL");

        if (!foreignKeyExists(connection, "Gallery", "gallery_admin_fk")) {
            executeIgnoringDuplicate(connection,
                    "ALTER TABLE Gallery ADD CONSTRAINT gallery_admin_fk FOREIGN KEY (admin_id) REFERENCES Administrator(admin_id)");
        }
    }

    private void seedFromContentLocationsIfEmpty(Connection connection) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM Gallery";
        try (PreparedStatement countStatement = connection.prepareStatement(countSql);
             ResultSet resultSet = countStatement.executeQuery()) {
            if (resultSet.next() && resultSet.getInt(1) > 0) {
                return;
            }
        }

        seedLocationsFromTable(connection, "Exhibition", "location");
        seedLocationsFromTable(connection, "Workshop", "location");
    }

    private void seedLocationsFromTable(Connection connection, String tableName, String locationColumn) throws SQLException {
        if (!tableExists(connection, tableName)) {
            return;
        }

        String seedSql = """
                INSERT IGNORE INTO Gallery(gallery_id, name, address, rating, admin_id)
                SELECT CONCAT('G', UPPER(LEFT(MD5(%s), 8))), %s, %s, 0, ?
                FROM %s
                WHERE %s IS NOT NULL AND %s <> ''
                GROUP BY %s
                """.formatted(locationColumn, locationColumn, locationColumn, tableName,
                locationColumn, locationColumn, locationColumn);

        try (PreparedStatement seedStatement = connection.prepareStatement(seedSql)) {
            seedStatement.setString(1, findAdminId(connection));
            seedStatement.executeUpdate();
        }
    }

    private String findAdminId(Connection connection) throws SQLException {
        ensureDefaultAdministrator(connection);

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT admin_id FROM Administrator ORDER BY admin_id LIMIT 1"
        );
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString("admin_id");
            }
        }

        throw new SQLException("No administrator available for Gallery.admin_id.");
    }

    private void ensureDefaultAdministrator(Connection connection) throws SQLException {
        try (PreparedStatement countStatement = connection.prepareStatement("SELECT COUNT(*) FROM Administrator");
             ResultSet resultSet = countStatement.executeQuery()) {
            if (resultSet.next() && resultSet.getInt(1) > 0) {
                return;
            }
        }

        try (PreparedStatement userStatement = connection.prepareStatement("""
                INSERT INTO UserAccount(user_id, email, name, password, role)
                VALUES ('UADMINDEFAULT', 'admin@artconnect.local', 'Gallery Admin', 'defaultPassword', 'ADMINISTRATOR')
                ON DUPLICATE KEY UPDATE role = VALUES(role)
                """);
             PreparedStatement adminStatement = connection.prepareStatement("""
                INSERT INTO Administrator(admin_id, admin_level, user_id)
                VALUES ('ADDEFAULT', 'DEFAULT', 'UADMINDEFAULT')
                ON DUPLICATE KEY UPDATE admin_level = VALUES(admin_level)
                """)) {
            userStatement.executeUpdate();
            adminStatement.executeUpdate();
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

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private boolean foreignKeyExists(Connection connection, String tableName, String constraintName) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND CONSTRAINT_NAME = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, constraintName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private void executeIgnoringDuplicate(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() != 1061 && e.getErrorCode() != 1826) {
                throw e;
            }
        }
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String generateId() {
        return "G" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
