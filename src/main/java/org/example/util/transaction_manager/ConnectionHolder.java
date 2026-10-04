package org.example.util.transaction_manager;

import java.sql.Connection;

public class ConnectionHolder {
    private static final ThreadLocal<Connection> current = new ThreadLocal<>();

    public static void set(Connection conn) {
        current.set(conn);
    }

    public static Connection get() {
        return current.get();
    }

    public static void remove() {
        current.remove();
    }
}
