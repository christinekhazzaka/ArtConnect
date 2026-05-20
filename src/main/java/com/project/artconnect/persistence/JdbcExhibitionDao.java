package com.project.artconnect.persistence;

import com.project.artconnect.dao.ExhibitionDao;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcExhibitionDao implements ExhibitionDao {

    @Override
    public List<Exhibition> findAll() {
        ensureTableExists();

        List<Exhibition> exhibitions = new ArrayList<>();
        String sql = """
                SELECT e.exhibition_id, e.title, e.description, e.event_date, e.capacity,
                       g.name AS gallery_name, g.address AS gallery_address, g.rating AS gallery_rating
                FROM Exhibition e
                JOIN Gallery g ON e.gallery_id = g.gallery_id
                ORDER BY e.event_date, e.title
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                exhibitions.add(mapExhibition(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while loading exhibitions.", e);
        }

        return exhibitions;
    }

    public Optional<Exhibition> findByTitle(String title) {
        ensureTableExists();

        String sql = """
                SELECT e.exhibition_id, e.title, e.description, e.event_date, e.capacity,
                       g.name AS gallery_name, g.address AS gallery_address, g.rating AS gallery_rating
                FROM Exhibition e
                JOIN Gallery g ON e.gallery_id = g.gallery_id
                WHERE e.title = ?
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapExhibition(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while finding exhibition by title.", e);
        }

        return Optional.empty();
    }

    @Override
    public void save(Exhibition exhibition) {
        ensureTableExists();

        String sql = """
                INSERT INTO Exhibition(exhibition_id, title, description, event_date, capacity, gallery_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, generateId("EX"));
            bindExhibition(statement, exhibition);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error while saving exhibition.", e);
        }
    }

    @Override
    public void update(Exhibition exhibition) {
        ensureTableExists();

        String sql = """
                UPDATE Exhibition
                SET description = ?, event_date = ?, capacity = ?, gallery_id = ?
                WHERE title = ?
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, safeText(exhibition.getDescription(), ""));
            statement.setDate(2, Date.valueOf(toLocalDate(exhibition.getStartDate())));
            statement.setInt(3, Math.max(1, exhibition.getCapacity()));
            statement.setString(4, findGalleryIdByName(connection, galleryName(exhibition)));
            statement.setString(5, exhibition.getTitle());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error while updating exhibition.", e);
        }
    }

    @Override
    public void delete(String title) {
        ensureTableExists();

        String sql = "DELETE FROM Exhibition WHERE title = ?";

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error while deleting exhibition.", e);
        }
    }

    private Exhibition mapExhibition(ResultSet resultSet) throws SQLException {
        Gallery gallery = new Gallery(
                resultSet.getString("gallery_name"),
                resultSet.getString("gallery_address"),
                resultSet.getDouble("gallery_rating")
        );
        Exhibition exhibition = new Exhibition(
                resultSet.getString("title"),
                toLocalDate(resultSet.getDate("event_date")),
                null,
                gallery
        );
        exhibition.setDescription(resultSet.getString("description"));
        exhibition.setCapacity(resultSet.getInt("capacity"));
        return exhibition;
    }

    private void bindExhibition(PreparedStatement statement, Exhibition exhibition) throws SQLException {
        statement.setString(2, exhibition.getTitle());
        statement.setString(3, safeText(exhibition.getDescription(), ""));
        statement.setDate(4, Date.valueOf(toLocalDate(exhibition.getStartDate())));
        statement.setInt(5, Math.max(1, exhibition.getCapacity()));
        statement.setString(6, findGalleryIdByName(statement.getConnection(), galleryName(exhibition)));
    }

    private void ensureTableExists() {
        new JdbcGalleryDao().findAll();

        String createSql = """
                CREATE TABLE IF NOT EXISTS Exhibition (
                    exhibition_id VARCHAR(50) NOT NULL PRIMARY KEY,
                    title VARCHAR(100) NOT NULL UNIQUE,
                    description TEXT NOT NULL,
                    event_date DATE NOT NULL,
                    capacity INT NOT NULL,
                    gallery_id VARCHAR(50) NOT NULL,
                    KEY idx_exhibition_gallery (gallery_id),
                    CONSTRAINT exhibition_gallery_fk FOREIGN KEY (gallery_id) REFERENCES Gallery(gallery_id),
                    CONSTRAINT chk_exhibition_capacity CHECK (capacity > 0)
                )
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(createSql)) {
            statement.executeUpdate();
            ensureGalleryIdColumn(connection);
            migrateLocationToGalleryId(connection);
            dropLocationColumnIfExists(connection);
            ensurePresentedInTable(connection);
        } catch (SQLException e) {
            throw new RuntimeException("Error while ensuring Exhibition table exists.", e);
        }
    }

    private void ensureGalleryIdColumn(Connection connection) throws SQLException {
        if (columnExists(connection, "gallery_id")) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "ALTER TABLE Exhibition ADD COLUMN gallery_id VARCHAR(50) NULL"
        )) {
            statement.executeUpdate();
        }
    }

    private void migrateLocationToGalleryId(Connection connection) throws SQLException {
        if (!columnExists(connection, "location")) {
            return;
        }

        String insertMissingGalleries = """
                INSERT IGNORE INTO Gallery(gallery_id, name, address, rating, admin_id)
                SELECT CONCAT('G', UPPER(LEFT(MD5(location), 8))), location, location, 0, ?
                FROM Exhibition
                WHERE location IS NOT NULL AND location <> ''
                GROUP BY location
                """;

        String updateGalleryIds = """
                UPDATE Exhibition e
                JOIN Gallery g ON g.name = e.location
                SET e.gallery_id = g.gallery_id
                WHERE e.gallery_id IS NULL
                """;

        try (PreparedStatement insertStatement = connection.prepareStatement(insertMissingGalleries);
             PreparedStatement updateStatement = connection.prepareStatement(updateGalleryIds)) {
            insertStatement.setString(1, findAdminId(connection));
            insertStatement.executeUpdate();
            updateStatement.executeUpdate();
        }

        fillMissingGalleryIds(connection);
        makeGalleryIdRequired(connection);
        addGalleryForeignKeyIfMissing(connection);
    }

    private void fillMissingGalleryIds(Connection connection) throws SQLException {
        String galleryId = findOrCreateDefaultGallery(connection);
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE Exhibition SET gallery_id = ? WHERE gallery_id IS NULL"
        )) {
            statement.setString(1, galleryId);
            statement.executeUpdate();
        }
    }

    private void makeGalleryIdRequired(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "ALTER TABLE Exhibition MODIFY gallery_id VARCHAR(50) NOT NULL"
        )) {
            statement.executeUpdate();
        }
    }

    private void dropLocationColumnIfExists(Connection connection) throws SQLException {
        if (!columnExists(connection, "location")) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "ALTER TABLE Exhibition DROP COLUMN location"
        )) {
            statement.executeUpdate();
        }
    }

    private void addGalleryForeignKeyIfMissing(Connection connection) throws SQLException {
        if (foreignKeyExists(connection, "exhibition_gallery_fk")) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "ALTER TABLE Exhibition ADD CONSTRAINT exhibition_gallery_fk FOREIGN KEY (gallery_id) REFERENCES Gallery(gallery_id)"
        )) {
            statement.executeUpdate();
        }
    }

    private String findGalleryIdByName(Connection connection, String galleryName) throws SQLException {
        String sql = "SELECT gallery_id FROM Gallery WHERE name = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, galleryName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("gallery_id");
                }
            }
        }

        String galleryId = "G" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String insertSql = """
                INSERT INTO Gallery(gallery_id, name, address, rating, admin_id)
                VALUES (?, ?, ?, 0, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setString(1, galleryId);
            statement.setString(2, galleryName);
            statement.setString(3, galleryName);
            statement.setString(4, findAdminId(connection));
            statement.executeUpdate();
        }

        return galleryId;
    }

    private void ensurePresentedInTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS presented_in (
                    artwork_id VARCHAR(50) NOT NULL,
                    exhibition_id VARCHAR(50) NOT NULL,
                    PRIMARY KEY (artwork_id, exhibition_id),
                    FOREIGN KEY (artwork_id) REFERENCES Artwork(artwork_id),
                    FOREIGN KEY (exhibition_id) REFERENCES Exhibition(exhibition_id)
                )
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private String findAdminId(Connection connection) throws SQLException {
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

    private String findOrCreateDefaultGallery(Connection connection) throws SQLException {
        return findGalleryIdByName(connection, "Gallery");
    }

    private boolean columnExists(Connection connection, String columnName) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'Exhibition'
                  AND COLUMN_NAME = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private boolean foreignKeyExists(Connection connection, String constraintName) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'Exhibition'
                  AND CONSTRAINT_NAME = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, constraintName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private String galleryName(Exhibition exhibition) {
        if (exhibition.getGallery() == null || exhibition.getGallery().getName() == null
                || exhibition.getGallery().getName().isBlank()) {
            return "Gallery";
        }
        return exhibition.getGallery().getName();
    }

    private LocalDate toLocalDate(LocalDate date) {
        return date != null ? date : LocalDate.now();
    }

    private LocalDate toLocalDate(Date date) {
        return date != null ? date.toLocalDate() : null;
    }

    private String safeText(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private String generateId(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
