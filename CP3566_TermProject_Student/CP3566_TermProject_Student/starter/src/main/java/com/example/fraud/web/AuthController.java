package com.example.fraud.web;

import com.example.fraud.model.User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthSupport auth;

    public AuthController(AuthSupport auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        User user = auth.requireLogin(username, password);

        return Map.of(
                "username", user.getUsername(),
                "role", user.getRole(),
                "message", "login successful"
        );
    }
}