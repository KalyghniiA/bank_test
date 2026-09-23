package org.example.repository;


import org.example.exceptions.RepositoryParamException;
import org.example.exceptions.SQLTransactionException;
import org.example.model.Person;
import org.example.util.Dictionaries;
import org.example.util.PersonStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankPersonRepository implements Repository<UUID, Person> {
    private final ConcurrentHashMap<UUID, Person> repository = new ConcurrentHashMap<>();

    @Override
    public void save(UUID uuid, Person item, Connection conn) throws SQLException {
        String sql = """
                insert into person
                    (id, first_name, surname, middle_name, birth_date, phone_number, email, status)
                values (?, ?, ?, ?, ?, ?, ?, ?);
                """;
        int statusId = Dictionaries.statusPersonDictionary.entrySet().stream()
                .filter(elem -> elem.getValue().equals(item.getStatus().getMessage()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new RepositoryParamException("Данный тип аккаунта не известен"));

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, uuid);
            ps.setString(2, item.getFirstName());
            ps.setString(3, item.getLastName());
            if (item.getMiddleName() == null) {
                ps.setObject(4, null, Types.VARCHAR);
            } else {
                ps.setString(4, item.getMiddleName());
            }
            ps.setDate(5, Date.valueOf(item.getBirthDate()));
            if (item.getPhoneNumber() == null) {
                ps.setObject(6, null, Types.VARCHAR);
            } else {
                ps.setObject(6, item.getPhoneNumber());
            }
            if (item.getEmail() == null) {
                ps.setObject(7, null, Types.VARCHAR);
            } else {
                ps.setObject(7, item.getEmail());
            }
            ps.setObject(8, statusId);

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(UUID uuid, Connection conn) throws SQLException {
        int statusDeleteId = Dictionaries.statusPersonDictionary.entrySet().stream()
                .filter(elem -> elem.getValue().equals("BLOCKED"))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() ->  new RepositoryParamException("id статуса удаления не найден, проверьте заполненность словаря"));

        String sql = """
                update person set status = ? where id = ?;\s
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, statusDeleteId);
            ps.setObject(2, uuid);
            ps.executeUpdate();
        }

    }

    @Override
    public Optional<Person> get(UUID uuid, Connection conn) throws SQLException {
        String sql = "select * from person where id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, uuid);
            ResultSet rs = ps.executeQuery();
            Person person = null;
            if (rs.next()) {
                person = mapping(rs);
            }

            return Optional.ofNullable(person);
        }
    }

    @Override
    public void update(Person oldItem, Person newItem, Connection conn) throws SQLTransactionException, SQLException {

            String sqlSelect = "select * from person where id = ?;";
            try ( PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
                Person oldPersonToDB = null;
                ps.setObject(1, oldItem.getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    oldPersonToDB = mapping(rs);
                }
                if (!oldItem.equals(oldPersonToDB))
                    throw new SQLTransactionException("Данные уже были изменены, попробуйте снова");

                String sqlUpdate = """
                        update person set\s
                                          first_name = ?,
                                          surname = ?,
                                          middle_name = ?,
                                          birth_date = ?,
                                          phone_number = ?,
                                          email = ?,
                                          status = ?
                        where id = ?;
                        """;
                int statusId = Dictionaries.statusPersonDictionary.entrySet().stream()
                        .filter(elem -> elem.getValue().equals(newItem.getStatus().getMessage()))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElseThrow(() -> new RepositoryParamException("Передан неизвестный статус"));

                try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                    psUpdate.setString(1, newItem.getFirstName());
                    psUpdate.setString(2, newItem.getLastName());
                    if (newItem.getMiddleName() == null) {
                        psUpdate.setObject(3, null, Types.VARCHAR);
                    } else {
                        psUpdate.setString(3, newItem.getMiddleName());
                    }
                    psUpdate.setDate(4, Date.valueOf(newItem.getBirthDate()));
                    if (newItem.getPhoneNumber() == null) {
                        psUpdate.setObject(5, null, Types.VARCHAR);
                    } else {
                        psUpdate.setString(5, newItem.getPhoneNumber());
                    }
                    if (newItem.getEmail() == null) {
                        psUpdate.setObject(6, null, Types.VARCHAR);
                    } else {
                        psUpdate.setString(6, newItem.getEmail());
                    }
                    psUpdate.setInt(7, statusId);

                    psUpdate.executeUpdate();

                }


            }

    }

    @Override
    public List<Person> getAll(Connection conn) {
        throw new UnsupportedOperationException("Данный метод недоступен, попробуйте методы с сужением");
    }

    private Person mapping(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        String firstName = rs.getString("first_name");
        String surname = rs.getString("surname");
        String middleName = rs.getString("middle_name");
        LocalDate birthDate = rs.getDate("birth_date").toLocalDate();
        String phoneNumber = rs.getString("phone_number");
        String email = rs.getString("email");
        PersonStatus status = PersonStatus.fromString(Dictionaries.statusPersonDictionary.get(rs.getInt("status")));

        return new Person(id, firstName, surname, middleName, birthDate, phoneNumber, email, status);
    }
}
