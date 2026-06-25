package com.astrochakra.accounting.web.dto;

import java.math.BigDecimal;

/** Response shape matching what the React client reads: { balance, liquid_reserve }. */
public record BalanceDto(BigDecimal balance, BigDecimal liquidReserve) {
}
