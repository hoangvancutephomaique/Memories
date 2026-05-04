package com.guestbook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards unknown routes to the React SPA so client-side routing works
 * when the app is served from the Spring Boot JAR in production.
 */
@Controller
public class SpaController {

    @RequestMapping(value = { "/", "/{path:[^\\.]*}", "/{path:[^\\.]*}/**" })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
