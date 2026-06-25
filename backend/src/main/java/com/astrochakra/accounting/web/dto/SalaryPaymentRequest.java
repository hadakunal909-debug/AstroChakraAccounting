package com.astrochakra.accounting.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** snake_case: resource_id, resource_name, pay_type, fund_source, fund_label, paid_by. */
public record SalaryPaymentRequest(
        UUID resourceId,
        String resourceName,
        String payType,
        BigDecimal amount,
        BigDecimal units,
        String fundSource,
        String fundLabel,
        String period,
        String note,
        String paidBy) {
}
