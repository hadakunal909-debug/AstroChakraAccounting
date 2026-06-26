package com.astrochakra.accounting.web.dto;

/** snake_case: requested_by, entity_type, entity_id, change_type. */
public record ChangeRequestRequest(
        String requestedBy,
        String entityType,
        Long entityId,
        String changeType,
        String description) {
}
