package org.example.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dto.PersonResponseDTO;
import org.example.exceptions.RepositoryItemExistsException;
import org.example.service.PersonService;
import org.example.util.JsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

public class MeServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(MeServlet.class);
    private JsonSupport json;
    private PersonService personService;

    @Override
    public void init() {
        json = (JsonSupport) getServletContext().getAttribute("json");
        personService = (PersonService) getServletContext().getAttribute("personService");
    }

    @Override
    public void doGet(HttpServletRequest req,  HttpServletResponse resp) throws IOException {
        log.info("получен запрос на получение данных пользователя");
        UUID personId = null;
        try {
            personId = (UUID) req.getAttribute("personId");
            PersonResponseDTO person = personService.findById(personId);
            json.write(resp, HttpServletResponse.SC_OK, person);
            log.info("Данные пользователя отправлены");
        } catch (RepositoryItemExistsException e) {
            log.info("Пользователя с id {} нет в базе", personId);
            json.writeError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Ошибка при получении клиента {}", personId, e);
            json.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Внутренняя ошибка");
        }
    }
}
