package org.example.util.transaction_manager;

import org.example.exceptions.TransactionManagerSQLException;
import org.example.util.ConnectionService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

public class JDBCTransactionManager implements TransactionManager {
    @Override
    public <T> T runInTransaction(Supplier<T> supplier) {
        try {
            try (Connection conn = ConnectionService.getConnection()) {
                ConnectionHolder.set(conn);
                try {
                    conn.setAutoCommit(false);

                    T result = supplier.get();

                    conn.commit();

                    return result;
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                } finally {
                    ConnectionHolder.remove();
                }
            }
        } catch (SQLException e) {
            throw new TransactionManagerSQLException("Произошла ошибка базы данных", e);
        }
    }

    @Override
    public void runInTransaction(Runnable runnable) {
        runInTransaction(() -> {
            runnable.run();
            return null;
        });
    }
}
