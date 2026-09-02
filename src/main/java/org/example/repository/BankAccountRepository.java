package org.example.repository;

import org.example.exceptions.RepositoryItemExistsexception;
import org.example.model.BankAccount;


import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankAccountRepository implements Repository<UUID, BankAccount> {
    private final ConcurrentHashMap<UUID, BankAccount> repository = new ConcurrentHashMap<>();

    @Override
    public void save(UUID id, BankAccount item) {
        BankAccount value = repository.putIfAbsent(id, item);
        if (value != null) throw new RepositoryItemExistsexception("Такой счет уже есть");
    }

    @Override
    public void delete(UUID id) {
        repository.remove(id);
    }

    @Override
    public Optional<BankAccount> get(UUID id) {
        return Optional.ofNullable(repository.get(id));
    }

    @Override
    public  List<BankAccount> getAll() {
        return repository.values().stream().toList();
    }
}
