package com.astrochakra.accounting.web.dto;

/** Any null field means "leave unchanged". A non-blank password is re-hashed. */
public record UpdateUserRequest(
        String displayName,
        String email,
        String role,
        Boolean isActive,
        String password) {
}
