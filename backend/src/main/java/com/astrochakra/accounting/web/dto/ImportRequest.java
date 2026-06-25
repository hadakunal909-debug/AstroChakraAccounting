package com.astrochakra.accounting.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** JSON: { "rows": [ {transaction...} ], "closing_balance": 99999 | null } */
public record ImportRequest(List<CreateTransactionRequest> rows, BigDecimal closingBalance) {
}
