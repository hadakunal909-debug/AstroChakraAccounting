package com.astrochakra.accounting.web.dto;

import java.math.BigDecimal;

/**
 * Partial update: either field may be null, mirroring the existing
 * updateBalance(balance, liquid) semantics where null means "leave unchanged".
 */
public record BalanceUpdateRequest(BigDecimal balance, BigDecimal liquidReserve) {
}
