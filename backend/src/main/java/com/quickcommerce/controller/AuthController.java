package com.quickcommerce.controller;

import com.quickcommerce.dto.AuthDtos.AuthResponse;
import com.quickcommerce.dto.AuthDtos.LoginRequest;
import com.quickcommerce.dto.AuthDtos.RegisterRequest;
import com.quickcommerce.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    /** Customer sign-up. Admins and delivery partners are created in the database. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }
}
