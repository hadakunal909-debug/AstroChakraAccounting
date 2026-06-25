package com.astrochakra.accounting.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.astrochakra.accounting.domain.AppUser;
import com.astrochakra.accounting.repository.AppUserRepository;
import com.astrochakra.accounting.security.JwtService;
import com.astrochakra.accounting.web.dto.LoginResponse;
import com.astrochakra.accounting.web.dto.UserDto;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(AppUserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public LoginResponse login(String username, String rawPassword) {
        AppUser u = users.findByUsername(username).orElse(null);
        if (u == null || Boolean.FALSE.equals(u.getIsActive()) || !passwordMatches(u, rawPassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        u.setLastLogin(Instant.now());
        users.save(u); // also persists a freshly-migrated BCrypt hash, if any

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", u.getRole() == null ? "regular" : u.getRole());
        claims.put("name", u.getDisplayName() == null ? "" : u.getDisplayName());
        String token = jwt.generateToken(u.getUsername(), claims);
        return new LoginResponse(token, UserDto.from(u));
    }

    @Transactional(readOnly = true)
    public UserDto me(String username) {
        AppUser u = users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return UserDto.from(u);
    }

    /**
     * Verifies the password. Existing rows store the password in plain text
     * (the legacy app), so on a successful plaintext match we transparently
     * re-hash it with BCrypt — passwords become hashed as users log in.
     */
    private boolean passwordMatches(AppUser u, String raw) {
        String stored = u.getPasswordHash();
        if (stored == null || raw == null) return false;
        if (stored.startsWith("$2")) {
            return encoder.matches(raw, stored);
        }
        if (stored.equals(raw)) {
            u.setPasswordHash(encoder.encode(raw));
            u.setUpdatedAt(Instant.now());
            return true;
        }
        return false;
    }
}
