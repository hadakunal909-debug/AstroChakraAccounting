package com.astrochakra.accounting.web.dto;

import java.math.BigDecimal;

/** JSON: { "current_stock": 42 } — sets stock directly (used by invoice deductions). */
public record StockSetRequest(BigDecimal currentStock) {
}
