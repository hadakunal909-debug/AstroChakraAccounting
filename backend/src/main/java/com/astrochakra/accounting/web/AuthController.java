package com.astrochakra.accounting.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.astrochakra.accounting.service.AuthService;
import com.astrochakra.accounting.web.dto.LoginRequest;
import com.astrochakra.accounting.web.dto.LoginResponse;
import com.astrochakra.accounting.web.dto.UserDto;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        return auth.login(req.username(), req.password());
    }

    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        return auth.me(authentication.getName());
    }
}
