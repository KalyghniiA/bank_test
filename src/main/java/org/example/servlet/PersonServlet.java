package org.example.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dto.PersonResponseDTO;
import org.example.exceptions.RepositoryItemExistsException;
import org.example.service.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

public class PersonServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(PersonServlet.class);
    private final PersonService personService;

    public PersonServlet(PersonService personService) {
        this.personService = personService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.info("Получен запрос GET. Начало обработки");
        String result = req.getPathInfo();
        resp.setContentType("text/plain;charset=utf-8");

        if (result == null) {
            log.info("Попытка получения списка пользователей");
            resp.setStatus(404);// пока без аутификации, в дальнейшем, при реализации аутификации надо будет сделать разбивку по типу клиента
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
                resp.setStatus(404);
            }

        }
    }

    private void getPerson(String id, HttpServletResponse resp) throws IOException {
        try {
            UUID uuid;
            try {
                uuid = UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                resp.setStatus(400);
                resp.getWriter().write("Не корректный id");
                return;
            }

            PersonResponseDTO result = personService.findById(uuid);

            resp.getWriter().write(result.toString()); //пока так, без перевода в json
        } catch (RepositoryItemExistsException e) {
            resp.setStatus(404);
            resp.getWriter().write(e.getMessage());
        }
    }
}
