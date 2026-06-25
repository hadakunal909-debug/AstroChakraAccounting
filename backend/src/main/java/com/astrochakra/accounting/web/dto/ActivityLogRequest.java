package com.astrochakra.accounting.web.dto;

import java.util.UUID;

/** snake_case: user_name, user_role, entity_type, entity_id. */
public record ActivityLogRequest(
        String userName,
        String userRole,
        String action,
        String entityType,
        UUID entityId,
        String description) {
}
