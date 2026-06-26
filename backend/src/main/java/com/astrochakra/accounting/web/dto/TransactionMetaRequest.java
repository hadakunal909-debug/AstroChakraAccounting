package com.astrochakra.accounting.web.dto;

import java.time.LocalDate;

/** Edit tags only — never amount/kind/fund_source/balance. */
public record TransactionMetaRequest(
        LocalDate date,
        String person,
        String category,
        String projectCode,
        String note) {
}
