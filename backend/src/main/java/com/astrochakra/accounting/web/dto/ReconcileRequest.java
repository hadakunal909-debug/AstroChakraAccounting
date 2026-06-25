package com.astrochakra.accounting.web.dto;

import java.math.BigDecimal;

/** JSON: { "true_balance": 125000 } */
public record ReconcileRequest(BigDecimal trueBalance) {
}
