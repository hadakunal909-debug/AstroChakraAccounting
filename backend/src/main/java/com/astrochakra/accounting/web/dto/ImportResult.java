package com.astrochakra.accounting.web.dto;

import java.math.BigDecimal;

public record ImportResult(int imported, BigDecimal newBalance) {
}
