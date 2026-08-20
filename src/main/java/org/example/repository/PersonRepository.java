package org.example.repository;

import org.example.exceptions.EmptyPersonException;
import org.example.model.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PersonRepository {
    private final ConcurrentHashMap<UUID, Person> persons = new ConcurrentHashMap<>();

    public Optional<Person> findById(UUID id) {
        return Optional.ofNullable(persons.get(id));
    }

    public void save(Person person) {
        persons.put(person.getId(), person);
    }

    public void delete(UUID id) {
        persons.remove(id);
    }

    public List<Person> findAll() {
        return new ArrayList<>(persons.values());
    }
}
