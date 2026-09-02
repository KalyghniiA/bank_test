package org.example.repository;

import org.example.exceptions.RepositoryItemExistsexception;
import org.example.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionRepository implements Repository<UUID, Transaction> {
    private final ConcurrentHashMap<UUID, Transaction> transactions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<UUID>> transactionForUsers = new ConcurrentHashMap<>();

    @Override
    public void save(UUID transactionId, Transaction item) {
        Transaction transaction = transactions.putIfAbsent(transactionId, item);
        if (transaction != null) throw new RepositoryItemExistsexception(String.format("Транзакция с id %s уже есть", transactionId));
        List<UUID> list = transactionForUsers.computeIfAbsent(item.getAccountId(), k -> new ArrayList<>());
        list.add(transactionId);
    }

    @Override
    public void delete(UUID uuid) {
        throw new UnsupportedOperationException("Транзакции нельзя удалить");
    }

    @Override
    public Optional<Transaction> get(UUID uuid) {
        return Optional.ofNullable(transactions.get(uuid));
    }

    @Override
    public List<Transaction> getAll() {
        return new ArrayList<>(transactions.values());
    }

    public List<Transaction> getByAccountId(UUID accountId) {
        if (!transactionForUsers.containsKey(accountId)) return new ArrayList<>();
        return transactionForUsers.get(accountId).stream().map(transactions::get).toList();
    }

}
