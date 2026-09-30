package org.example.service;

import org.example.dto.AccountRequestDTO;
import org.example.dto.CredentialsRequestDTO;
import org.example.dto.PersonRequestDTO;
import org.example.exceptions.PersonAuthException;
import org.example.exceptions.RepositoryItemExistsException;
import org.example.model.BankAccount;
import org.example.model.Credentials;
import org.example.model.Person;
import org.example.repository.CredentialsRepository;
import org.example.repository.Repository;
import org.example.util.AccountType;
import org.example.util.PasswordService;
import org.example.util.transaction_manager.TransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class PersonServiceTest {
    @Mock
    Repository<UUID, Person> personRepository;
    @Mock
    Repository<UUID, BankAccount> bankAccountRepository;
    @Mock
    CredentialsRepository<UUID, Credentials> credentialsRepository;
    @Mock
    TransactionManager transactionManager;

    PersonService personService;

    @BeforeEach
    void setUp() {
        personService = new PersonService(personRepository, bankAccountRepository, credentialsRepository, transactionManager);
    }

    @Nested
    @DisplayName("Успешные сценарии")
    class SuccessfulOperations {
        @BeforeEach
        void setUp() {
            doAnswer(invocation ->  {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(transactionManager).runInTransaction(any(Runnable.class));
        }

        @Test
        @DisplayName("Проверка создания клиента")
        void checkCreatePerson() {
            PersonRequestDTO person = new PersonRequestDTO("firstName", "subname", null, LocalDate.of(1990,12, 1), "9111111111", null);
            CredentialsRequestDTO credentials = new CredentialsRequestDTO("login", "secret");

            personService.createPerson(person, credentials);

            ArgumentCaptor<Person> captorPerson = ArgumentCaptor.forClass(Person.class);
            ArgumentCaptor<Credentials> captorCredentials = ArgumentCaptor.forClass(Credentials.class);

            verify(personRepository, times(1)).save(any(UUID.class), captorPerson.capture());
            verify(credentialsRepository, times(1)).save(any(UUID.class), captorCredentials.capture());

            assertAll(
                    () -> assertEquals(person.firstName(), captorPerson.getValue().getFirstName()),
                    () -> assertEquals(person.subName(), captorPerson.getValue().getSubName()),
                    () -> assertEquals(person.middleName(), captorPerson.getValue().getMiddleName()),
                    () -> assertEquals(person.birthDate(), captorPerson.getValue().getBirthDate()),
                    () -> assertEquals(person.number(), captorPerson.getValue().getPhoneNumber()),
                    () -> assertEquals(person.email(), captorPerson.getValue().getEmail()),
                    () -> assertEquals(credentials.login(), captorCredentials.getValue().login())
            );

        }

        @Test
        @DisplayName("проверка создания счета")
        void checkCreateAccountToPerson() {
            Person person = new Person("firstName", "subname", null, LocalDate.of(1990,12, 1), "9111111111", null);
            AccountRequestDTO accDto = new AccountRequestDTO(person.getId(), BigDecimal.ZERO, AccountType.DEFAULT.getMessage(), null, null);

            when(personRepository.get(eq(person.getId()))).thenReturn(Optional.of(person));

            personService.createAccountToPerson(person.getId(), accDto);
            ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);

            verify(bankAccountRepository, times(1)).save(any(UUID.class), captor.capture());

            assertAll(
                    () -> assertEquals(accDto.personId(), captor.getValue().getUserId()),
                    () -> assertEquals(accDto.balance(), captor.getValue().getBalance()),
                    () -> assertEquals(accDto.type(), captor.getValue().getAccountType().getMessage())
            );
        }

        @Test
        @DisplayName("проверка удаления счета")
        void checkDeleteAccountToPerson() {
            Person person = new Person("firstName", "subname", null, LocalDate.of(1990,12, 1), "9111111111", null);
            BankAccount account = new BankAccount(UUID.randomUUID(), person.getId(), BigDecimal.ZERO);

            when(personRepository.get(eq(person.getId()))).thenReturn(Optional.of(person));


            personService.deleteAccountToPerson(person.getId(), account.getId());

            verify(personRepository, times(1)).get(eq(person.getId()));
            verify(bankAccountRepository, times(1)).delete(eq(account.getId()));
        }
    }

    @Nested
    @DisplayName("Тестирование авторизации")
    class AuthorizedOperations {
        byte[] salt;
        int iterations;

        @BeforeEach
        @SuppressWarnings("unchecked")
        void setUp() {
            salt = PasswordService.generateSalt();
            iterations = 10;

            when(transactionManager.runInTransaction(any(Supplier.class))).thenAnswer(invocation -> {
                        Supplier<?> supplier =  invocation.getArgument(0);
                        return supplier.get();
                    });
        }

        @Test
        @DisplayName("Проверка авторизации")
        void checkLogin() {
            Person person = new Person("firstName", "subname", null, LocalDate.of(1990,12, 1), "9111111111", null);
            CredentialsRequestDTO credDto = new CredentialsRequestDTO("login", "secret");
            byte[] passwordHash = PasswordService.hashPassword(credDto.password(), salt, iterations);
            Credentials cred = new Credentials(person.getId(), credDto.login(), passwordHash, salt, iterations);

            when(personRepository.get(eq(person.getId()))).thenReturn(Optional.of(person));
            when(credentialsRepository.getByLogin(credDto.login())).thenReturn(Optional.of(cred));

            Person result = personService.login(credDto);

            verify(credentialsRepository, times(1)).getByLogin(credDto.login());
            verify(personRepository, times(1)).get(eq(person.getId()));

            assertEquals(person.getId(), result.getId());
        }

        @Test
        @DisplayName("Авторизация при отсутствующем логине")
        void checkAuthorisationNotLoginToBase() {
            CredentialsRequestDTO credDto = new CredentialsRequestDTO("login", "secret");

            when(credentialsRepository.getByLogin(eq(credDto.login()))).thenReturn(Optional.empty());
            PersonAuthException exception = assertThrows(PersonAuthException.class, () -> personService.login(credDto));

            verify(personRepository, never()).get(any());

            assertEquals("Данный пользователь не найден", exception.getMessage());
        }

        @Test
        @DisplayName("Неверный пароль")
        void checkAuthorisationIncorrectPassword() {
            CredentialsRequestDTO incorrectCredDto = new CredentialsRequestDTO("login", "secret1");
            CredentialsRequestDTO correctCredDto = new CredentialsRequestDTO("login", "secret");
            byte[] successPasswordHash = PasswordService.hashPassword(correctCredDto.password(), salt, iterations);

            Credentials cred = new Credentials(UUID.randomUUID(), correctCredDto.login(), successPasswordHash, salt, iterations);

            when(credentialsRepository.getByLogin(eq(incorrectCredDto.login()))).thenReturn(Optional.of(cred));

            PersonAuthException exception = assertThrows(PersonAuthException.class, () -> personService.login(incorrectCredDto));

            verify(personRepository, never()).get(any());

            assertEquals("Пароль не верен", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Негативные сценарии")
    class NegativeOperations {

        @BeforeEach
        void setUp() {
            doAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(transactionManager).runInTransaction(any(Runnable.class));
        }

        @Test
        @DisplayName("Попытка создания счета несуществующему пользователю")
        void checkCreateAccountToNonexistentPerson() {
            UUID personId = UUID.randomUUID();
            AccountRequestDTO accountRequestDTO = new AccountRequestDTO(personId, BigDecimal.ZERO, AccountType.DEFAULT.getMessage(), null, null);

            when(personRepository.get(eq(personId))).thenReturn(Optional.empty());

            RepositoryItemExistsException exception = assertThrows(RepositoryItemExistsException.class, () -> personService.createAccountToPerson(personId, accountRequestDTO));
            verify(bankAccountRepository, never()).save(any(UUID.class), any(BankAccount.class));

            assertEquals("Данного пользователя нет в базе", exception.getMessage());
        }

        @Test
        @DisplayName("попытка удаления счета у несуществующего пользователя")
        void checkDeleteAccountToNonexistentPerson() {
            UUID personId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();

            when(personRepository.get(eq(personId))).thenReturn(Optional.empty());

            RepositoryItemExistsException exception = assertThrows(RepositoryItemExistsException.class, () -> personService.deleteAccountToPerson(personId, accountId));

            verify(bankAccountRepository, never()).delete(any());
            assertEquals("Данного пользователя нет в базе",  exception.getMessage());
        }
    }
}
