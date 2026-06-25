package com.astrochakra.accounting.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inventory_products")
@Getter
@Setter
public class InventoryProduct {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "sku")
    private String sku;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "unit")
    private String unit;

    @Column(name = "buying_price")
    private BigDecimal buyingPrice;

    @Column(name = "selling_price")
    private BigDecimal sellingPrice;

    @Column(name = "min_stock")
    private BigDecimal minStock;

    @Column(name = "current_stock")
    private BigDecimal currentStock;

    @Column(name = "description")
    private String description;

    @Column(name = "hsn_code")
    private String hsnCode;

    @Column(name = "tax_percent")
    private BigDecimal taxPercent;

    @Column(name = "product_type")
    private String productType;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
