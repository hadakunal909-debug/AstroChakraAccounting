package com.astrochakra.accounting.web.dto;

public record CreateUserRequest(
        String username,
        String password,
        String displayName,
        String email,
        String role) {
}
