package org.example.service;

import org.example.dto.AccountRequestDTO;
import org.example.dto.CredentialsRequestDTO;
import org.example.dto.PersonRequestDTO;
import org.example.exceptions.*;
import org.example.mapper.AccountMapper;
import org.example.mapper.CredentialsMapper;
import org.example.mapper.PersonMapper;
import org.example.model.BankAccount;
import org.example.model.Credentials;
import org.example.model.Person;
import org.example.repository.BankAccountRepository;
import org.example.repository.BankCredentialsRepository;
import org.example.repository.BankPersonRepository;

import org.example.util.ConnectionService;
import org.example.util.PasswordService;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public class PersonService {
    private final BankPersonRepository bankPersonRepository;
    private final BankAccountRepository bankAccountRepository;
    private final BankCredentialsRepository bankCredentialsRepository;

    public PersonService(BankPersonRepository bankPersonRepository,
                         BankAccountRepository bankAccountRepository,
                         BankCredentialsRepository bankCredentialsRepository) {
        this.bankPersonRepository = bankPersonRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.bankCredentialsRepository = bankCredentialsRepository;
    }

    public void createPerson(PersonRequestDTO personDTO, CredentialsRequestDTO credDto) {
        try (Connection conn = ConnectionService.getConnection()) {
                conn.setAutoCommit(false);
                Person person = null;
                try {
                    person = PersonMapper.toPerson(personDTO);
                    bankPersonRepository.save(person.getId(), person, conn);
                }  catch (SQLException e) {
                    conn.rollback();
                    switch (e.getSQLState()) {
                        case "23505" : throw new RepositoryParamException("Пользователь с таким id уже есть");
                        default: throw new RuntimeException("Другая ошибка базы", e);
                    }
                }

                try {
                    Credentials cred = CredentialsMapper.generateCredentials(credDto, person.getId());
                    bankCredentialsRepository.save(cred.id(), cred, conn);
                } catch (SQLException e) {
                    conn.rollback();
                    switch (e.getSQLState()) {
                        case "23505": throw new CredentialsUniqueException("Либо данный пользователь уже регистрировался, либо такой логин уже есть. Попробуйте другие данные");
                        case "23502": throw new RepositoryParamException("Передано пустое значение");
                        default: throw new RuntimeException("Другая ошибка базы", e);
                    }
                }
                conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Другая ошибка базы", e);
        }

    }

    public void createAccountToPerson(UUID personId, AccountRequestDTO accountDTO) {
        try (Connection conn = ConnectionService.getConnection()) {
            try {
                conn.setAutoCommit(false);

                Optional<Person> person = bankPersonRepository.get(personId, conn);
                if (person.isPresent()) {
                    BankAccount account = AccountMapper.toBankAccount(accountDTO);

                    bankAccountRepository.save(account.getId(), account, conn);
                } else {
                    throw new RepositoryItemExistsException("Данного пользователя нет в базе");
                }
                conn.commit();
            } catch (SQLException | RepositoryItemExistsException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            switch (e.getSQLState()) {
                case "23503": throw new RepositoryItemExistsException("Данного пользователя нет в базе");
                case "23505": throw new RepositoryParamException("Данный счет уже есть в базе");
                default: throw new RuntimeException("Другая ошибка базы", e);
            }
        }
    }

    public void deleteAccountToPerson(UUID personId, UUID accountId) {
        try (Connection conn = ConnectionService.getConnection()) {
            try {
                conn.setAutoCommit(false);
                Optional<Person> person = bankPersonRepository.get(personId, conn);
                if (person.isPresent()) {
                    bankAccountRepository.delete(accountId, conn);
                } else {
                    throw new RepositoryItemExistsException("Данного пользователя нет в базе");
                }

                conn.commit();
            } catch (SQLException | RepositoryItemExistsException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            switch (e.getSQLState()) {
                case "23503": throw new RepositoryItemExistsException("Данного счета нет в базе");
                default: throw new RuntimeException("Другая ошибка базы", e);
            }
        }
    }

    public Person login(CredentialsRequestDTO credDto) {
        try (Connection conn = ConnectionService.getConnection()) {
             try {
                 conn.setAutoCommit(false);
                 Credentials cred = bankCredentialsRepository.getByLogin(credDto.login(), conn).orElseThrow(() -> new PersonAuthException("Данный пользователь не найден"));
                 byte[] salt = cred.salt();
                 int iterations = cred.iterations();

                 byte[] passwordHash = PasswordService.hashPassword(credDto.password(), salt, iterations);
                 if (!MessageDigest.isEqual(cred.passwordHash(), passwordHash)) throw new PersonAuthException("Пароль не верен");

                 return bankPersonRepository.get(cred.personId(), conn).orElseThrow(() -> new RepositoryItemExistsException("Пользователя нет в базе"));
             } catch (SQLException | RepositoryItemExistsException | PersonAuthException e) {
                 conn.rollback();
                 throw e;
             }
        } catch (SQLException e) {
            switch (e.getSQLState()) {
                case "23503": throw new RepositoryItemExistsException("Данного пользователя нет в базе");
                default: throw new RuntimeException("Другая ошибка базы", e);
            }
        }
    }

    //пока отложу, нужно продумать как менять статус, без потери предыдущего состояния

//    public void changeStatusToPerson(UUID personId, String status) {
//        try (Connection conn = ConnectionService.getConnection()) {
//            conn.setAutoCommit(false);
//            try {
//                Person person = bankPersonRepository.get(personId, conn).orElseThrow(() -> new RepositoryItemExistsException("Данного пользователя нет"));
//
//
//
//
//            } catch (SQLException | IllegalArgumentException | SQLTransactionException e) {
//                conn.rollback();
//            }
//        } catch (SQLException e) {
//            switch (e.getSQLState()) {
//                case "23503": throw new RepositoryItemExistsException("Данного пользователя нет в базе");
//                default: throw new RuntimeException("Другая ошибка базы", e);
//            }
//        } catch (IllegalArgumentException e) {
//            throw new RepositoryParamException("Передан не верный параметр статуса");
//        }
//    }


}
