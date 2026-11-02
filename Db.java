package com.prajwal.tickets.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens JDBC connections. Settings come from environment variables so that no
 * password is ever written in the source code (or pushed to GitHub):
 *
 *   DB_URL       e.g. jdbc:mysql://localhost:3306/ticketdb
 *   DB_USER      e.g. root
 *   DB_PASSWORD  your database password
 */
public final class Db {

    private Db() {
    }

    public static boolean isConfigured() {
        String url = System.getenv("DB_URL");
        return url != null && !url.isBlank();
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getenv("DB_URL");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }
}
