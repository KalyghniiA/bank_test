package org.example.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface Repository<ID, T> {
    public void save(ID id, T item, Connection connection) throws SQLException;

    public void delete(ID id, Connection connection) throws SQLException;

    public Optional<T> get(ID id, Connection connection) throws SQLException;

    public void update(T oldItem, T newItem, Connection connection) throws SQLException;

    public List<T> getAll(Connection connection) throws SQLException;
}
