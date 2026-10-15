package com.andretti101.escolaweb.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.HandlerMapping;

@Hidden
@Controller
public class TemplateController {

    @GetMapping({
            "/login.html",
            "/forgot-password.html",
            "/reset-password.html",
            "/ai-chat.html",
            "/{dir:principal|secretary|teacher|student|components}/{page:[a-zA-Z0-9_-]+}.html",
            "/{dir:principal|secretary|teacher|student|components}/{sub:[a-zA-Z0-9_-]+}/{page:[a-zA-Z0-9_-]+}.html"
    })
    public String handleHtml(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        return path.substring(1, path.length() - 5);
    }
}
