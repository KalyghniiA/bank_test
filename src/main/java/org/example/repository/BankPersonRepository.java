package org.example.repository;


import org.example.exceptions.RepositoryItemExistsexception;
import org.example.model.Person;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankPersonRepository implements Repository<UUID, Person> {
    private final ConcurrentHashMap<UUID, Person> repository = new ConcurrentHashMap<>();

    @Override
    public void save(UUID uuid, Person item) {
        Person value = repository.putIfAbsent(uuid, item);
        if (value != null) throw new RepositoryItemExistsexception("Такой пользователь уже есть");
    }

    @Override
    public void delete(UUID uuid) {
        repository.remove(uuid);
    }

    @Override
    public Optional<Person> get(UUID uuid) {
        return Optional.ofNullable(repository.get(uuid));
    }

    @Override
    public List<Person> getAll() {
        return repository.values().stream().toList();
    }
}
