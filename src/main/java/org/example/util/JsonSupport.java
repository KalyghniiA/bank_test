package org.example.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dto.ErrorMessageResponseDTO;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

public class JsonSupport {
    private final JsonMapper mapper;

    public JsonSupport(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public <T> T read(HttpServletRequest req, Class<T> type) throws IOException {
        return mapper.readValue(req.getInputStream(), type);
    }

    public void write(HttpServletResponse resp, int status, Object obj) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(status);
        mapper.writeValue(resp.getOutputStream(), obj);
    }

    public void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        write(resp, status, new ErrorMessageResponseDTO(message));
    }

    public boolean isJson(HttpServletRequest req) {
        return req.getContentType() != null && req.getContentType().toLowerCase().startsWith("application/json");
    }

}
