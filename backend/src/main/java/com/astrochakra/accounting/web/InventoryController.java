package com.astrochakra.accounting.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.astrochakra.accounting.domain.InventoryCategory;
import com.astrochakra.accounting.domain.InventoryProduct;
import com.astrochakra.accounting.domain.InventoryTransaction;
import com.astrochakra.accounting.repository.InventoryCategoryRepository;
import com.astrochakra.accounting.repository.InventoryProductRepository;
import com.astrochakra.accounting.repository.InventoryTransactionRepository;
import com.astrochakra.accounting.service.StockService;
import com.astrochakra.accounting.web.dto.InvCategoryRequest;
import com.astrochakra.accounting.web.dto.InvProductRequest;
import com.astrochakra.accounting.web.dto.StockMovementRequest;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryCategoryRepository categories;
    private final InventoryProductRepository products;
    private final InventoryTransactionRepository txns;
    private final StockService stock;

    public InventoryController(InventoryCategoryRepository categories, InventoryProductRepository products,
                               InventoryTransactionRepository txns, StockService stock) {
        this.categories = categories;
        this.products = products;
        this.txns = txns;
        this.stock = stock;
    }

    // ===== Categories =====
    @GetMapping("/categories")
    public List<InventoryCategory> listCategories() {
        return categories.findAllByOrderByNameAsc();
    }

    @PostMapping("/categories")
    public InventoryCategory createCategory(@RequestBody InvCategoryRequest r) {
        InventoryCategory c = new InventoryCategory();
        c.setId(UUID.randomUUID());
        c.setName(r.name());
        c.setDescription(r.description() == null ? "" : r.description());
        c.setColor(r.color() == null ? "#6366f1" : r.color());
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return categories.save(c);
    }

    @PutMapping("/categories/{id}")
    public InventoryCategory updateCategory(@PathVariable UUID id, @RequestBody InvCategoryRequest r) {
        InventoryCategory c = categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        c.setName(r.name());
        c.setDescription(r.description() == null ? "" : r.description());
        c.setColor(r.color() == null ? "#6366f1" : r.color());
        c.setUpdatedAt(Instant.now());
        return categories.save(c);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable UUID id) {
        categories.deleteById(id);
    }

    // ===== Products =====
    @GetMapping("/products")
    public List<InventoryProduct> listProducts() {
        return products.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/products")
    public InventoryProduct createProduct(@RequestBody InvProductRequest r) {
        InventoryProduct p = new InventoryProduct();
        p.setId(UUID.randomUUID());
        apply(p, r);
        p.setCurrentStock(java.math.BigDecimal.ZERO);
        p.setIsActive(Boolean.TRUE);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return products.save(p);
    }

    @PutMapping("/products/{id}")
    public InventoryProduct updateProduct(@PathVariable UUID id, @RequestBody InvProductRequest r) {
        InventoryProduct p = products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        apply(p, r);
        p.setUpdatedAt(Instant.now());
        return products.save(p);
    }

    /** Soft delete (is_active = false). */
    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable UUID id) {
        products.findById(id).ifPresent(p -> {
            p.setIsActive(Boolean.FALSE);
            p.setUpdatedAt(Instant.now());
            products.save(p);
        });
    }

    /** Set stock directly (used by invoice line-item deductions). */
    @PutMapping("/products/{id}/stock")
    public InventoryProduct setStock(@PathVariable UUID id, @RequestBody com.astrochakra.accounting.web.dto.StockSetRequest r) {
        InventoryProduct p = products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        p.setCurrentStock(r.currentStock());
        p.setUpdatedAt(Instant.now());
        return products.save(p);
    }

    // ===== Transactions =====
    @GetMapping("/transactions")
    public List<InventoryTransaction> listTransactions(@RequestParam(defaultValue = "50") int limit) {
        return txns.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.max(1, limit)));
    }

    @GetMapping("/transactions/by-product/{productId}")
    public List<InventoryTransaction> byProduct(@PathVariable UUID productId) {
        return txns.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @PostMapping("/stock-movement")
    public InventoryTransaction stockMovement(@RequestBody StockMovementRequest req) {
        return stock.move(req);
    }

    private void apply(InventoryProduct p, InvProductRequest r) {
        p.setName(r.name());
        p.setSku(r.sku() == null ? "" : r.sku());
        p.setCategoryId(r.categoryId());
        p.setCategoryName(r.categoryName() == null ? "" : r.categoryName());
        p.setUnit(r.unit() == null ? "pcs" : r.unit());
        p.setBuyingPrice(r.buyingPrice());
        p.setSellingPrice(r.sellingPrice());
        p.setMinStock(r.minStock());
        p.setDescription(r.description() == null ? "" : r.description());
        p.setHsnCode(r.hsnCode() == null ? "" : r.hsnCode());
        p.setTaxPercent(r.taxPercent());
        p.setProductType(r.productType() == null ? "Product" : r.productType());
    }
}
