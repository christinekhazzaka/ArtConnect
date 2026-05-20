package com.project.artconnect.persistence;

import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcCommunityMemberDao implements CommunityMemberDao {

    @Override
    public Optional<CommunityMember> findByName(String name) {
        ensureCommunityColumns();
        String sql = """
                SELECT cm.member_id, cm.join_date, cm.city, cm.membership_type, u.user_id, u.name, u.email
                FROM CommunityMember cm
                JOIN UserAccount u ON cm.user_id = u.user_id
                WHERE u.name = ?
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapMember(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while finding community member by name.", e);
        }

        return Optional.empty();
    }

    @Override
    public List<CommunityMember> findAll() {
        ensureCommunityColumns();
        List<CommunityMember> members = new ArrayList<>();
        String sql = """
                SELECT cm.member_id, cm.join_date, cm.city, cm.membership_type, u.user_id, u.name, u.email
                FROM CommunityMember cm
                JOIN UserAccount u ON cm.user_id = u.user_id
                ORDER BY u.name
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                members.add(mapMember(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while loading community members.", e);
        }

        return members;
    }

    @Override
    public void save(CommunityMember member) {
        ensureCommunityColumns();
        String userId = generateId("U");
        String memberId = generateId("M");

        String insertUser = """
                INSERT INTO UserAccount(user_id, email, name, password, role)
                VALUES (?, ?, ?, ?, 'COMMUNITY_MEMBER')
                """;
        String insertMember = """
                INSERT INTO CommunityMember(member_id, join_date, city, membership_type, user_id)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement userStatement = connection.prepareStatement(insertUser);
                 PreparedStatement memberStatement = connection.prepareStatement(insertMember)) {
                userStatement.setString(1, userId);
                userStatement.setString(2, safeEmail(member.getEmail(), userId));
                userStatement.setString(3, member.getName());
                userStatement.setString(4, "defaultPassword");
                userStatement.executeUpdate();

                memberStatement.setString(1, memberId);
                memberStatement.setDate(2, Date.valueOf(LocalDate.now()));
                memberStatement.setString(3, safeText(member.getCity(), "Unknown"));
                memberStatement.setString(4, normalizeMembershipType(member.getMembershipType()));
                memberStatement.setString(5, userId);
                memberStatement.executeUpdate();

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while saving community member.", e);
        }
    }

    @Override
    public void update(CommunityMember member) {
        ensureCommunityColumns();
        String sql = """
                UPDATE UserAccount u
                JOIN CommunityMember cm ON cm.user_id = u.user_id
                SET u.email = ?, u.name = ?, cm.city = ?, cm.membership_type = ?
                WHERE u.name = ?
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, safeEmail(member.getEmail(), "member"));
            statement.setString(2, member.getName());
            statement.setString(3, safeText(member.getCity(), "Unknown"));
            statement.setString(4, normalizeMembershipType(member.getMembershipType()));
            statement.setString(5, member.getName());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error while updating community member.", e);
        }
    }

    @Override
    public void delete(String name) {
        ensureCommunityColumns();
        String findSql = """
                SELECT cm.member_id, u.user_id
                FROM CommunityMember cm
                JOIN UserAccount u ON cm.user_id = u.user_id
                WHERE u.name = ?
                """;

        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement findStatement = connection.prepareStatement(findSql)) {
                findStatement.setString(1, name);

                String memberId;
                String userId;
                try (ResultSet resultSet = findStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        return;
                    }
                    memberId = resultSet.getString("member_id");
                    userId = resultSet.getString("user_id");
                }

                executeDeleteIfTableExists(connection, "WorkshopBooking",
                        "DELETE FROM WorkshopBooking WHERE member_id = ?", memberId);
                executeDelete(connection, "DELETE FROM CommunityMember WHERE member_id = ?", memberId);
                executeDelete(connection, "DELETE FROM UserAccount WHERE user_id = ?", userId);

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while deleting community member.", e);
        }
    }

    private CommunityMember mapMember(ResultSet resultSet) throws SQLException {
        CommunityMember member = new CommunityMember(resultSet.getString("name"), resultSet.getString("email"));
        member.setCity(resultSet.getString("city"));
        member.setMembershipType(normalizeMembershipType(resultSet.getString("membership_type")));
        return member;
    }

    private String normalizeMembershipType(String membershipType) {
        if (membershipType == null || membershipType.isBlank()) {
            return "BASIC";
        }

        String normalized = membershipType.trim().toUpperCase();
        return switch (normalized) {
            case "PREMIUM" -> "PREMIUM";
            case "BASIC", "FREE", "COMMUNITY" -> "BASIC";
            default -> throw new IllegalArgumentException("Membership must be BASIC or PREMIUM.");
        };
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeEmail(String email, String fallback) {
        if (email == null || email.isBlank()) {
            return fallback.toLowerCase() + "@artconnect.local";
        }
        return email;
    }

    private String generateId(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void ensureCommunityColumns() {
        try (Connection connection = ConnectionManager.getConnection()) {
            ensureColumnExists(connection, "city", "VARCHAR(100) NULL");
            ensureColumnExists(connection, "membership_type", "VARCHAR(100) NULL");
            executeUpdate(connection, "UPDATE CommunityMember SET city = 'Unknown' WHERE city IS NULL OR city = ''");
            executeUpdate(connection, "UPDATE CommunityMember SET membership_type = 'BASIC' WHERE membership_type IS NULL OR membership_type = '' OR UPPER(membership_type) IN ('COMMUNITY', 'FREE')");
            executeUpdate(connection, "UPDATE CommunityMember SET membership_type = UPPER(membership_type)");
            executeUpdate(connection, "ALTER TABLE CommunityMember MODIFY city VARCHAR(100) NOT NULL");
            executeUpdate(connection, "ALTER TABLE CommunityMember MODIFY membership_type VARCHAR(100) NOT NULL");
        } catch (SQLException e) {
            throw new RuntimeException("Error while ensuring CommunityMember columns exist.", e);
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
                  AND TABLE_NAME = 'CommunityMember'
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
                "ALTER TABLE CommunityMember ADD COLUMN " + columnName + " " + definition
        )) {
            alterStatement.executeUpdate();
        }
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
