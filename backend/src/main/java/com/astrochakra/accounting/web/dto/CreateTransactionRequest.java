package com.astrochakra.accounting.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** JSON keys are snake_case (project_code, bill_url, is_reversal, fund_source, ...). */
public record CreateTransactionRequest(
        LocalDate date,
        String projectCode,
        String person,
        BigDecimal amount,
        String kind,
        String category,
        String note,
        String billUrl,
        String billName,
        Boolean isReversal,
        Long originalId,
        String reversesKind,
        String fundSource) {
}
