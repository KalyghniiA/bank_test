package org.example.web;

import jakarta.servlet.*;
import org.example.filter.AuthFilter;
import org.example.filter.RequestIdFilter;
import org.example.servlet.AuthServlet;
import org.example.servlet.HealthServlet;
import org.example.servlet.PersonServlet;
import org.example.filter.EncodingFilter;

import java.util.EnumSet;
import java.util.Set;

public class BankWebInitializer implements ServletContainerInitializer {
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        ctx.addListener(new BankContextListener());

        ctx.setSessionTrackingModes(EnumSet.of(SessionTrackingMode.COOKIE));
        var sessionConfig = ctx.getSessionCookieConfig();
        sessionConfig.setHttpOnly(true);
        sessionConfig.setAttribute("SameSite", "Strict");

        FilterRegistration.Dynamic encoding = ctx.addFilter("encoding", new EncodingFilter());
        encoding.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
        FilterRegistration.Dynamic requestId = ctx.addFilter("requestId", new RequestIdFilter());
        requestId.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
        FilterRegistration.Dynamic authFilter = ctx.addFilter("auth", new AuthFilter());
        authFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/api/*");

        ServletRegistration.Dynamic health = ctx.addServlet("health", new HealthServlet());
        health.addMapping("/api/health");

        ServletRegistration.Dynamic person = ctx.addServlet("person", new PersonServlet());
        person.addMapping("/api/persons/*");

        ServletRegistration.Dynamic auth = ctx.addServlet("auth", new AuthServlet());
        auth.addMapping("/api/auth/*");

    }
}
