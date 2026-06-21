package com.example.fraud.web;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @PostMapping("/login")
    public Map<String, String> login() {
        return Map.of(
                "message", "login endpoint ready"
        );
    }
}