package org.example.repository;

import org.example.exceptions.RepositoryException;
import org.example.exceptions.SQLTransactionException;
import org.example.model.Person;
import org.example.util.Dictionaries;
import org.example.util.PersonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class JDBCBankPersonRepositoryIT extends AbstractRepositoryIT {
    private final JDBCBankPersonRepository personRepository = new JDBCBankPersonRepository();

    @BeforeEach
    void setUp() throws SQLException {
        // Клиенты-фикстуры PERSON_1/PERSON_2 из базового класса остаются; удаляем только созданных в тестах.
        executeUpdate(String.format("delete from person where id not in ('%s', '%s')", PERSON_1, PERSON_2));
    }

    @Test
    @DisplayName("save сохраняет все поля клиента")
    void checkSavePerson() throws SQLException {
        Person person = newPerson("Мария", "Иванова", "Петровна", "9111111111", "maria@mail.ru");

        inTransaction(() -> personRepository.save(person.getId(), person));

        PersonRow row = readPersonRow(person.getId());
        assertAll(
                () -> assertNotNull(row, "Строка person не создана"),
                () -> assertEquals("Мария", row.firstName()),
                () -> assertEquals("Иванова", row.surname()),
                () -> assertEquals("Петровна", row.middleName()),
                () -> assertEquals(LocalDate.of(1995, 5, 20), row.birthDate()),
                () -> assertEquals("9111111111", row.phoneNumber()),
                () -> assertEquals("maria@mail.ru", row.email()),
                () -> assertEquals(personStatusId(PersonStatus.ACTIVE), row.status())
        );
    }

    @Test
    @DisplayName("save сохраняет необязательные поля как NULL")
    void checkSavePersonWithNullOptionalFields() throws SQLException {
        Person person = newPerson("Олег", "Сидоров", null, null, null);

        inTransaction(() -> personRepository.save(person.getId(), person));

        PersonRow row = readPersonRow(person.getId());
        assertAll(
                () -> assertNull(row.middleName()),
                () -> assertNull(row.phoneNumber()),
                () -> assertNull(row.email())
        );
    }

    @Test
    @DisplayName("save с уже существующим id — RepositoryException с причиной 23505 (нарушение уникальности)")
    void checkSaveDuplicateId() {
        Person duplicate = new Person(PERSON_1, "Дубль", "Дублев", null, LocalDate.of(2000, 1, 1), null, null, PersonStatus.ACTIVE);

        RepositoryException exception = assertThrows(RepositoryException.class,
                () -> inTransaction(() -> personRepository.save(duplicate.getId(), duplicate)));

        assertEquals("23505", assertInstanceOf(PSQLException.class, exception.getCause()).getSQLState());
    }

    @Test
    @DisplayName("get восстанавливает клиента")
    void checkGetPerson() throws SQLException {
        Person person = newPerson("Мария", "Иванова", "Петровна", "9111111111", "maria@mail.ru");
        insertPerson(person);

        Person answer = inTransaction(() -> personRepository.get(person.getId())).orElseThrow();

        assertEquals(person, answer);
    }

    @Test
    @DisplayName("get несуществующего клиента — пустой Optional")
    void checkGetUnknownPerson() {
        Optional<Person> answer = inTransaction(() -> personRepository.get(UUID.randomUUID()));

        assertTrue(answer.isEmpty());
    }

    @Test
    @DisplayName("update меняет поля клиента")
    void checkUpdatePerson() throws SQLException {
        Person person = newPerson("Мария", "Иванова", null, null, null);
        insertPerson(person);
        Person changed = new Person(person.getId(), "Мария", "Петрова", "Сергеевна", person.getBirthDate(),
                "9222222222", "new@mail.ru", PersonStatus.BLOCKED);

        inTransaction(() -> {
            Person current = personRepository.get(person.getId()).orElseThrow();
            personRepository.update(current, changed);
        });

        Person answer = inTransaction(() -> personRepository.get(person.getId())).orElseThrow();
        assertEquals(changed, answer);
    }

    @Test
    @DisplayName("update по устаревшей версии — SQLTransactionException, данные не меняются")
    void checkUpdateRejectsStaleVersion() throws SQLException {
        Person person = newPerson("Мария", "Иванова", null, null, null);
        insertPerson(person);
        Person stale = new Person(person.getId(), "Устаревшее", "Имя", null, person.getBirthDate(), null, null, PersonStatus.ACTIVE);
        Person changed = new Person(person.getId(), "Новое", "Имя", null, person.getBirthDate(), null, null, PersonStatus.ACTIVE);

        assertThrows(SQLTransactionException.class,
                () -> inTransaction(() -> personRepository.update(stale, changed)));

        assertEquals("Мария", readPersonRow(person.getId()).firstName());
    }

    @Test
    @DisplayName("delete — мягкое удаление: статус BLOCKED, строка остаётся")
    void checkDeletePerson() throws SQLException {
        Person person = newPerson("Мария", "Иванова", null, null, null);
        insertPerson(person);

        inTransaction(() -> personRepository.delete(person.getId()));

        PersonRow row = readPersonRow(person.getId());
        assertAll(
                () -> assertNotNull(row),
                () -> assertEquals(personStatusId(PersonStatus.BLOCKED), row.status())
        );
    }

    @Test
    @DisplayName("getAll не поддерживается")
    void checkGetAllIsUnsupported() {
        assertThrows(UnsupportedOperationException.class, personRepository::getAll);
    }

    // ---------- вспомогательные методы (прямой SQL) ----------

    private record PersonRow(String firstName, String surname, String middleName, LocalDate birthDate,
                             String phoneNumber, String email, int status) {
    }

    private static Person newPerson(String firstName, String surname, String middleName, String phone, String email) {
        return new Person(UUID.randomUUID(), firstName, surname, middleName, LocalDate.of(1995, 5, 20),
                phone, email, PersonStatus.ACTIVE);
    }

    private static int personStatusId(PersonStatus status) {
        return idOf(Dictionaries.statusPersonDictionary, status.getMessage());
    }

    private static void insertPerson(Person person) throws SQLException {
        String sql = """
                insert into person (id, first_name, surname, middle_name, birth_date, phone_number, email, status)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DATA_SOURCE.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, person.getId());
            ps.setString(2, person.getFirstName());
            ps.setString(3, person.getLastName());
            ps.setString(4, person.getMiddleName());
            ps.setObject(5, person.getBirthDate());
            ps.setString(6, person.getPhoneNumber());
            ps.setString(7, person.getEmail());
            ps.setInt(8, personStatusId(person.getStatus()));
            ps.executeUpdate();
        }
    }

    private static PersonRow readPersonRow(UUID id) throws SQLException {
        String sql = "select first_name, surname, middle_name, birth_date, phone_number, email, status from person where id = ?";
        try (Connection conn = DATA_SOURCE.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new PersonRow(rs.getString("first_name"), rs.getString("surname"), rs.getString("middle_name"),
                        rs.getObject("birth_date", LocalDate.class), rs.getString("phone_number"),
                        rs.getString("email"), rs.getInt("status"));
            }
        }
    }
}
