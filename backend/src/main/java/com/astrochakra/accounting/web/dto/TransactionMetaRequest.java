package com.astrochakra.accounting.web.dto;

/** Edit tags only — never amount/kind/fund_source/balance. */
public record TransactionMetaRequest(
        String date,
        String person,
        String category,
        String projectCode,
        String note) {
}
