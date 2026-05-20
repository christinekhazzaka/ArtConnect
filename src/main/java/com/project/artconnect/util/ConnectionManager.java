package com.project.artconnect.util;

import com.project.artconnect.config.DatabaseConfig;


import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;


/**
 * Utility class to manage JDBC connections.
 * TODO: Students must implementation the getConnection logic.
 */
public class ConnectionManager {

    /**
     * Provides a connection to the MySQL database.
     * 
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.URL,
                DatabaseConfig.USER,
                DatabaseConfig.PASSWORD
        );
    }
}
