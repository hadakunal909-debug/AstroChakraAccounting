package com.astrochakra.accounting.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.astrochakra.accounting.domain.Balance;
import com.astrochakra.accounting.domain.InventoryProduct;
import com.astrochakra.accounting.domain.InventoryTransaction;
import com.astrochakra.accounting.repository.BalanceRepository;
import com.astrochakra.accounting.repository.InventoryProductRepository;
import com.astrochakra.accounting.repository.InventoryTransactionRepository;
import com.astrochakra.accounting.repository.ProjectRepository;
import com.astrochakra.accounting.web.dto.StockMovementRequest;

/**
 * Atomic stock movements. stock_in reduces the bank balance (and reserve/project
 * allocation when sourced from those), increases stock, and records the movement —
 * all in one transaction. stock_out/adjustment only change stock.
 */
@Service
public class StockService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final InventoryProductRepository products;
    private final InventoryTransactionRepository invTxns;
    private final BalanceRepository balances;
    private final ProjectRepository projects;

    public StockService(InventoryProductRepository products, InventoryTransactionRepository invTxns,
                        BalanceRepository balances, ProjectRepository projects) {
        this.products = products;
        this.invTxns = invTxns;
        this.balances = balances;
        this.projects = projects;
    }

    @Transactional
    public InventoryTransaction move(StockMovementRequest r) {
        InventoryProduct p = products.findById(r.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        BigDecimal qty = nz(r.quantity());
        String type = r.type() == null ? "stock_in" : r.type();
        BigDecimal total = nz(r.totalCost());

        if ("stock_in".equals(type)) {
            String fund = r.fundSource() == null ? "available" : r.fundSource();
            Balance b = loadOrCreateBalance();
            b.setBalance(nz(b.getBalance()).subtract(total));
            if ("reserve".equals(fund)) {
                b.setLiquidReserve(maxZero(nz(b.getLiquidReserve()).subtract(total)));
            } else if (isProjectFund(fund)) {
                projects.findByCode(fund).ifPresent(pr -> {
                    pr.setAllocated(maxZero(nz(pr.getAllocated()).subtract(total)));
                    pr.setUpdatedAt(Instant.now());
                    projects.save(pr);
                });
            }
            b.setUpdatedAt(Instant.now());
            balances.save(b);
            p.setCurrentStock(nz(p.getCurrentStock()).add(qty));
        } else if ("stock_out".equals(type)) {
            p.setCurrentStock(maxZero(nz(p.getCurrentStock()).subtract(qty)));
        } else { // adjustment: set absolute
            p.setCurrentStock(qty);
        }
        p.setUpdatedAt(Instant.now());
        products.save(p);

        InventoryTransaction it = new InventoryTransaction();
        it.setId(UUID.randomUUID());
        it.setProductId(p.getId());
        it.setProductName(p.getName());
        it.setType(type);
        it.setQuantity(qty);
        it.setUnitCost(r.unitCost());
        it.setTotalCost("stock_in".equals(type) ? total : ZERO);
        it.setFundSource("stock_in".equals(type) ? r.fundSource() : "");
        it.setFundLabel(r.fundLabel());
        it.setNotes(r.notes());
        it.setPerformedBy(r.performedBy());
        it.setCreatedAt(Instant.now());
        return invTxns.save(it);
    }

    private Balance loadOrCreateBalance() {
        return balances.findAll().stream().findFirst().orElseGet(() -> {
            Balance nb = new Balance();
            nb.setId(UUID.randomUUID());
            nb.setBalance(ZERO);
            nb.setLiquidReserve(ZERO);
            return nb;
        });
    }

    private static boolean isProjectFund(String fund) {
        return fund != null && !"available".equals(fund) && !"reserve".equals(fund);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? ZERO : v;
    }

    private static BigDecimal maxZero(BigDecimal v) {
        return v.signum() < 0 ? ZERO : v;
    }
}
