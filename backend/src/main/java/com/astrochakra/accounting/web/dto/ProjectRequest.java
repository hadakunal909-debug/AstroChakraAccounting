package com.astrochakra.accounting.web.dto;

import java.math.BigDecimal;

/** JSON keys (snake_case): code, name, fixed_budget, allocated, color. */
public record ProjectRequest(
        String code,
        String name,
        BigDecimal fixedBudget,
        BigDecimal allocated,
        String color) {
}
