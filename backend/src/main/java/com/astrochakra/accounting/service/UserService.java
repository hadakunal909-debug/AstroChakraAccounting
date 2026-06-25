package com.astrochakra.accounting.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.astrochakra.accounting.domain.AppUser;
import com.astrochakra.accounting.repository.AppUserRepository;
import com.astrochakra.accounting.web.dto.CreateUserRequest;
import com.astrochakra.accounting.web.dto.UpdateUserRequest;
import com.astrochakra.accounting.web.dto.UserDto;

@Service
public class UserService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public UserService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return users.findAll().stream().map(UserDto::from).toList();
    }

    @Transactional
    public UserDto create(CreateUserRequest r) {
        if (users.existsByUsername(r.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setUsername(r.username());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setDisplayName(r.displayName());
        u.setEmail(r.email() == null ? "" : r.email());
        u.setRole(r.role() == null ? "regular" : r.role());
        u.setIsActive(Boolean.TRUE);
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        return UserDto.from(users.save(u));
    }

    @Transactional
    public UserDto update(UUID id, UpdateUserRequest r) {
        AppUser u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (r.displayName() != null) u.setDisplayName(r.displayName());
        if (r.email() != null) u.setEmail(r.email());
        if (r.role() != null) u.setRole(r.role());
        if (r.isActive() != null) u.setIsActive(r.isActive());
        if (r.password() != null && !r.password().isBlank()) u.setPasswordHash(encoder.encode(r.password()));
        u.setUpdatedAt(Instant.now());
        return UserDto.from(users.save(u));
    }

    @Transactional
    public void delete(UUID id) {
        users.deleteById(id);
    }
}
