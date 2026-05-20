package com.project.artconnect.service.impl;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcWorkshopService implements WorkshopService {
    private final WorkshopDao workshopDao;

    public JdbcWorkshopService(WorkshopDao workshopDao) {
        this.workshopDao = workshopDao;
    }

    @Override
    public List<Workshop> getAllWorkshops() {
        return workshopDao.findAll();
    }

    @Override
    public Optional<Workshop> getWorkshopByTitle(String title) {
        return workshopDao.findByTitle(title);
    }

    @Override
    public void save(Workshop workshop) {
        workshopDao.save(workshop);
    }

    @Override
    public void update(Workshop workshop) {
        if (workshopDao.findByTitle(workshop.getTitle()).isPresent()) {
            workshopDao.update(workshop);
        } else {
            workshopDao.save(workshop);
        }
    }

    @Override
    public void delete(String workshopTitle) {
        workshopDao.delete(workshopTitle);
    }

    @Override
    public void bookWorkshop(Workshop workshop, CommunityMember member) {
        if (workshop == null || member == null) {
            return;
        }

        ensureBookingTableExists();

        String sql = """
                INSERT INTO WorkshopBooking(booking_id, workshop_id, member_id, booking_date, payment_status)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, generateId("B"));
            statement.setString(2, findWorkshopIdByTitle(workshop.getTitle()));
            statement.setString(3, findMemberIdByName(member.getName()));
            statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(5, "PENDING");
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error while booking workshop.", e);
        }
    }

    @Override
    public List<Booking> getBookingsByMember(CommunityMember member) {
        if (member == null) {
            return List.of();
        }

        ensureBookingTableExists();

        List<Booking> bookings = new ArrayList<>();
        String sql = """
                SELECT w.title, wb.booking_date, wb.payment_status
                FROM WorkshopBooking wb
                JOIN Workshop w ON wb.workshop_id = w.workshop_id
                JOIN CommunityMember cm ON wb.member_id = cm.member_id
                JOIN UserAccount u ON cm.user_id = u.user_id
                WHERE u.name = ?
                ORDER BY wb.booking_date DESC
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, member.getName());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Optional<Workshop> workshop = workshopDao.findByTitle(resultSet.getString("title"));
                    if (workshop.isPresent()) {
                        Booking booking = new Booking(workshop.get(), member);
                        Timestamp bookingDate = resultSet.getTimestamp("booking_date");
                        if (bookingDate != null) {
                            booking.setBookingDate(bookingDate.toLocalDateTime());
                        }
                        booking.setPaymentStatus(resultSet.getString("payment_status"));
                        bookings.add(booking);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while loading workshop bookings.", e);
        }

        return bookings;
    }

    private void ensureBookingTableExists() {
        String sql = """
                CREATE TABLE IF NOT EXISTS WorkshopBooking (
                    booking_id VARCHAR(50) NOT NULL PRIMARY KEY,
                    workshop_id VARCHAR(50) NOT NULL,
                    member_id VARCHAR(50) NOT NULL,
                    booking_date DATETIME NOT NULL,
                    payment_status VARCHAR(100) NOT NULL,
                    KEY idx_workshop_booking_workshop (workshop_id),
                    KEY idx_workshop_booking_member (member_id),
                    CONSTRAINT workshop_booking_workshop_fk FOREIGN KEY (workshop_id) REFERENCES Workshop(workshop_id),
                    CONSTRAINT workshop_booking_member_fk FOREIGN KEY (member_id) REFERENCES CommunityMember(member_id),
                    CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING', 'PAID', 'CANCELLED', 'REFUNDED'))
                )
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
            alignBookingColumns(connection);
        } catch (SQLException e) {
            throw new RuntimeException("Error while ensuring WorkshopBooking table exists.", e);
        }
    }

    private void alignBookingColumns(Connection connection) throws SQLException {
        executeUpdate(connection, "UPDATE WorkshopBooking SET payment_status = 'PENDING' WHERE payment_status IS NULL OR payment_status = ''");
        executeUpdate(connection, "ALTER TABLE WorkshopBooking MODIFY booking_date DATETIME NOT NULL");
        executeUpdate(connection, "ALTER TABLE WorkshopBooking MODIFY payment_status VARCHAR(100) NOT NULL");
        executeUpdate(connection, "ALTER TABLE WorkshopBooking MODIFY member_id VARCHAR(50) NOT NULL");
        executeUpdate(connection, "ALTER TABLE WorkshopBooking MODIFY workshop_id VARCHAR(50) NOT NULL");
    }

    private void executeUpdate(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private String findWorkshopIdByTitle(String title) throws SQLException {
        return findSingleId("""
                SELECT workshop_id
                FROM Workshop
                WHERE title = ?
                """, title, "Workshop not found: " + title);
    }

    private String findMemberIdByName(String name) throws SQLException {
        return findSingleId("""
                SELECT cm.member_id
                FROM CommunityMember cm
                JOIN UserAccount u ON cm.user_id = u.user_id
                WHERE u.name = ?
                """, name, "Community member not found: " + name);
    }

    private String findSingleId(String sql, String value, String notFoundMessage) throws SQLException {
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }

        throw new SQLException(notFoundMessage);
    }

    private String generateId(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
