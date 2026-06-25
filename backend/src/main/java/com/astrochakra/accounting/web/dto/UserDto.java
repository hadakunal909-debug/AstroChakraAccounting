package com.astrochakra.accounting.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.astrochakra.accounting.domain.AppUser;

/** Public view of a user (never includes the password hash). */
public record UserDto(
        UUID id,
        String username,
        String displayName,
        String email,
        String role,
        Boolean isActive,
        Instant lastLogin,
        Instant createdAt) {

    public static UserDto from(AppUser u) {
        return new UserDto(u.getId(), u.getUsername(), u.getDisplayName(), u.getEmail(),
                u.getRole(), u.getIsActive(), u.getLastLogin(), u.getCreatedAt());
    }
}
