package org.example.servlet;


import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.example.dto.CredentialsRequestDTO;
import org.example.dto.FieldErrorDTO;
import org.example.dto.PersonResponseDTO;
import org.example.exceptions.PersonAuthException;
import org.example.exceptions.RepositoryException;
import org.example.exceptions.RepositoryItemExistsException;
import org.example.exceptions.TransactionManagerSQLException;
import org.example.mapper.PersonMapper;
import org.example.model.Person;
import org.example.service.PersonService;
import org.example.util.JsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class AuthServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(AuthServlet.class);
    private JsonSupport json;
    private Validator validator;
    private PersonService personService;

    @Override
    public void init() {
        this.json = (JsonSupport) getServletContext().getAttribute("json");
        this.validator = (Validator) getServletContext().getAttribute("validator");
        this.personService = (PersonService) getServletContext().getAttribute("personService");
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.info("Получен запрос на страницу авторизации");
        String pathInfo = req.getPathInfo();

        if (pathInfo == null) {
            log.info("Обращение к неизвестному ресурсу");
            json.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Данного ресурса нет");
            return;
        }

        String[] info =  pathInfo.split("/");

        if (info.length != 2) {
            log.info("Обращение к неизвестному ресурсу");
            json.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Данного ресурса нет");
            return;
        }

        switch (info[1]) {
            case "login" -> login(req, resp);
            case "logout" -> logout(req, resp);
            default -> {
                json.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Данного ресурса нет");
            }
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.info("Получен запрос на авторизацию");
        try {
            CredentialsRequestDTO credentials = json.read(req, CredentialsRequestDTO.class);
            if (credentials == null) {
                log.info("Передан пустой объект");
                json.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Передан пустой объект");
                return;
            }

            Set<ConstraintViolation<CredentialsRequestDTO>> violations = validator.validate(credentials);
            if (!violations.isEmpty()) {
                List<FieldErrorDTO> errors = violations.stream()
                        .map(v -> new FieldErrorDTO(v.getPropertyPath().toString(), v.getMessage()))
                        .sorted(Comparator.comparing(FieldErrorDTO::field))
                        .toList();
                StringBuilder errorMsg = new StringBuilder("Возникли ошибки ввода данных, возникшие ошибки: ");
                for (FieldErrorDTO error : errors) {
                    errorMsg.append(error.field());
                    errorMsg.append(": ");
                    errorMsg.append(error.message());
                    errorMsg.append("; ");
                }
                log.warn(errorMsg.toString());
                json.writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Логин или пароль не верен");
                return;
            }



            Person person = personService.login(credentials);
            HttpSession session = req.getSession();
            req.changeSessionId();
            session.setAttribute("personId", person.getId());
            session.setMaxInactiveInterval(15 * 60);
            PersonResponseDTO personDto = PersonMapper.toPersonResponseDTO(person);
            json.write(resp, HttpServletResponse.SC_OK, personDto);
            log.info("Авторизация завершена");

        } catch (PersonAuthException e) {
            log.warn("Ошибка авторизации: не верные логин или пароль");
            json.writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Логин или пароль не верен");
        } catch (RepositoryItemExistsException e) {
            log.error("Ошибка авторизации: Клиента нет в базе, при наличия данных авторизации", e);
            json.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ошибка авторизации, попробуйте позже");
        } catch (RepositoryException | TransactionManagerSQLException e) {
            log.error("Ошибка работы с данными - {}", e.getMessage(), e);
            json.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ошибка авторизации, попробуйте позже");
        } catch (JacksonException e) {
            log.info("Ошибка чтения данных клиента");
            json.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "невозможно обработать данные");
        } catch (RuntimeException e) {
            log.error("Ошибка авторизации - {}", e.getMessage(), e);
            json.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Внутренняя ошибка");
        }
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.info("Получен запрос на выход");
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        log.info("Пользователь вышел");
    }
}
