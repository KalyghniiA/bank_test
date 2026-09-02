package org.example.repository;

import java.util.List;
import java.util.Optional;

public interface Repository<ID, T> {
    public void save(ID id, T item);

    public void delete(ID id);

    public Optional<T> get(ID id);

    public List<T> getAll();
}
