package com.app.web.config;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ApiDocRedirectController {

    @GetMapping({
        "/swagger-ui",
        "/swagger-ui/",
        "/swagger-ui.html",
        "/swagger-ui/index.html"
    })
    public String redirectSwaggerUi() {
        return "redirect:/doc.html";
    }

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}
