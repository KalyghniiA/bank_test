package org.example.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.util.JsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AuthFilter extends HttpFilter {
    private static final Logger log =  LoggerFactory.getLogger(AuthFilter.class);
    private JsonSupport json;

    @Override
    public void init() {
        json =  (JsonSupport) getServletContext().getAttribute("json");
    }

    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (isPublic(request)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("personId") == null) {
            json.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Требуется аутентификация");
            log.info("Ошибка аутентификации");
            return;
        }

        request.setAttribute("personId", session.getAttribute("personId"));

        chain.doFilter(request, response);

    }

    private boolean isPublic(HttpServletRequest req) {
        String[] paths = req.getServletPath().split("/");
        if (paths.length < 3) {
            return false;
        }

        String method = req.getMethod();

        if (paths[2].equals("health")) {
            return true;
        }

        if (paths[2].equals("auth")
                && req.getPathInfo() != null
                && ("/login".equals(req.getPathInfo()) || "/logout".equals(req.getPathInfo()))) {
            return true;
        }

        if (paths[2].equals("persons") && method.equals("POST") && req.getPathInfo() == null) {
            return true;
        }

        return false;
    }
}
