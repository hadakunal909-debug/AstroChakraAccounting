package com.astrochakra.accounting.web.dto;

import java.math.BigDecimal;

/** snake_case: pay_type, pay_amount, commission_percent, job_label. */
public record ResourceRequest(
        String name,
        String role,
        String payType,
        BigDecimal payAmount,
        BigDecimal commissionPercent,
        String jobLabel) {
}
