package org.example.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionService {
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                String.format("jdbc:postgresql://%s:%s/%s",
                        System.getenv("DB_HOST"),
                        System.getenv("DB_PORT"),
                        System.getenv("DB_NAME")),
                System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD")
        );
    }
}
