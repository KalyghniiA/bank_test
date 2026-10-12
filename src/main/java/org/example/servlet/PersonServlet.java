package org.example.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.example.dto.*;
import org.example.exceptions.CredentialsUniqueException;
import org.example.exceptions.RepositoryException;
import org.example.exceptions.RepositoryItemExistsException;
import org.example.exceptions.TransactionManagerSQLException;
import org.example.service.PersonService;
import org.example.util.JsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PersonServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(PersonServlet.class);
    private final PersonService personService;
    private final JsonSupport json;
    private final Validator validator;

    public PersonServlet(PersonService personService, JsonSupport json, Validator validator) {
        this.personService = personService;
        this.json = json;
        this.validator = validator;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.info("Получен запрос GET. Начало обработки");
        String result = req.getPathInfo();

        if (result == null) {
            log.info("Попытка получения списка пользователей");
            json.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Данного ресурса нет");// пока без аутификации, в дальнейшем, при реализации аутификации надо будет сделать разбивку по типу клиента
            return;
        }

        String[] info = result.split("/");

        switch (info.length) {
            case 2: {
                log.info("Получение пользователя по id {}", info[1]);
                getPerson(info[1], resp);
                break;
            }
            default: {
                json.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Данного ресурса нет");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.info("Получен POST запрос, начало отработки");
        String path = req.getPathInfo();
        // пока нет других методов, считаем что post только регистрирует и другие пути не поддерживает
        if (path != null) {
            log.info("Попытка отправки на не зарегистрированный путь");
            json.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "На данный ресурс нельзя отправлять");
            return;
        }

        if (!json.isJson(req)) {
            log.info("Попытка записи неподдерживаемого формата, формат {}", req.getContentType());
            json.writeError(resp, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Данный формат не поддерживается");
            return;
        }


        registration(req, resp);
    }

    private void getPerson(String id, HttpServletResponse resp) throws IOException {
        try {
            UUID uuid;
            try {
                uuid = UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                json.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Некорректный id");
                return;
            }

            PersonResponseDTO result = personService.findById(uuid);

            json.write(resp, HttpServletResponse.SC_OK, result);
        } catch (RepositoryItemExistsException e) {
            log.info("Пользователя с id {} нет в базе", id);
            json.writeError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Ошибка при получении клиента {}", id, e);
            json.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Внутренняя ошибка");
        }
    }

    private void registration(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            RegistrationRequestDTO info = json.read(req, RegistrationRequestDTO.class);
            if (info == null) {
                log.info("Передан пустой объект");
                json.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Передан пустой объект");
                return;
            }

            Set<ConstraintViolation<RegistrationRequestDTO>> violations = validator.validate(info);
            if (!violations.isEmpty()) {
                List<FieldErrorDTO> errors = violations.stream()
                        .map(v -> new FieldErrorDTO(v.getPropertyPath().toString(), v.getMessage()))
                        .sorted(Comparator.comparing(FieldErrorDTO::field))     // Set не упорядочен — отсортируем для стабильности
                        .toList();
                json.write(resp, HttpServletResponse.SC_BAD_REQUEST,
                        new ValidationErrorResponseDTO("Некорректные данные", errors));
                return;
            }

            UUID idPerson = personService.createPerson(info.person(), info.credentials());
            resp.setHeader("Location", "/api/person/" + idPerson);
            json.write(resp, HttpServletResponse.SC_CREATED, new CreatedResponseDTO(idPerson));

        } catch (JacksonException e) {
            log.info("Ошибка чтения данных клиента");
            json.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "невозможно обработать данные");
        } catch (RepositoryException | TransactionManagerSQLException e) {
            log.error(e.getMessage(), e);
            json.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ошибка сохранения, попробуйте позже");
        } catch (CredentialsUniqueException e) {
            log.info("Логин занят");
            json.writeError(resp, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Ошибка регистрации клиента", e);
            json.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Внутренняя ошибка");
        }
    }
}
