package com.astrochakra.accounting.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** JSON keys are snake_case (project_code, bill_url, is_reversal, fund_source, ...). */
public record CreateTransactionRequest(
        String date,
        String projectCode,
        String person,
        BigDecimal amount,
        String kind,
        String category,
        String note,
        String billUrl,
        String billName,
        Boolean isReversal,
        UUID originalId,
        String reversesKind,
        String fundSource) {
}
