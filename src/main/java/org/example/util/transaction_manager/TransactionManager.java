package org.example.util.transaction_manager;

import java.util.function.Supplier;

public interface TransactionManager {
    <T> T runInTransaction(Supplier<T> supplier);

    void runInTransaction(Runnable runnable);
}
