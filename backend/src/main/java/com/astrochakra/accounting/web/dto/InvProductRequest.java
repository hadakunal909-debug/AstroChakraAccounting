package com.astrochakra.accounting.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** snake_case: category_id, category_name, buying_price, selling_price, min_stock, hsn_code, tax_percent, product_type. */
public record InvProductRequest(
        String name,
        String sku,
        UUID categoryId,
        String categoryName,
        String unit,
        BigDecimal buyingPrice,
        BigDecimal sellingPrice,
        BigDecimal minStock,
        String description,
        String hsnCode,
        BigDecimal taxPercent,
        String productType) {
}
