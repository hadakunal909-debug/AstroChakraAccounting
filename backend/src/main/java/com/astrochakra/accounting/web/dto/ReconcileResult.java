package com.astrochakra.accounting.web.dto;

import java.math.BigDecimal;

public record ReconcileResult(int normalizedCount, BigDecimal adjustment, BigDecimal newBalance) {
}
