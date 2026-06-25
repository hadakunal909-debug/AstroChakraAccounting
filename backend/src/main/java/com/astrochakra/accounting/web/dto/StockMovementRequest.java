package com.astrochakra.accounting.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** type: stock_in | stock_out | adjustment. snake_case: product_id, unit_cost, total_cost, fund_source, fund_label, performed_by. */
public record StockMovementRequest(
        UUID productId,
        String type,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal totalCost,
        String fundSource,
        String fundLabel,
        String notes,
        String performedBy) {
}
