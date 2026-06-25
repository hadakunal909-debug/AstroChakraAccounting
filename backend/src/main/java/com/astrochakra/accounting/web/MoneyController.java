package com.astrochakra.accounting.web;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.astrochakra.accounting.domain.Transaction;
import com.astrochakra.accounting.service.MoneyService;
import com.astrochakra.accounting.web.dto.CreateTransactionRequest;
import com.astrochakra.accounting.web.dto.ImportRequest;
import com.astrochakra.accounting.web.dto.ImportResult;
import com.astrochakra.accounting.web.dto.ReconcileRequest;
import com.astrochakra.accounting.web.dto.ReconcileResult;
import com.astrochakra.accounting.web.dto.SettleRequest;

/**
 * Atomic money operations (each is a single DB transaction). These are the
 * endpoints the UI should use for money flows; the /api/transactions CRUD
 * endpoints remain for listing and tag-only edits.
 */
@RestController
@RequestMapping("/api/money")
public class MoneyController {

    private final MoneyService money;

    public MoneyController(MoneyService money) {
        this.money = money;
    }

    @PostMapping("/transaction")
    public Transaction record(@RequestBody CreateTransactionRequest req) {
        return money.record(req);
    }

    @PostMapping("/transaction/{id}/settle")
    public Transaction settle(@PathVariable UUID id, @RequestBody(required = false) SettleRequest req) {
        return money.settle(id, req == null ? null : req.note());
    }

    @PostMapping("/reconcile")
    public ReconcileResult reconcile(@RequestBody ReconcileRequest req) {
        return money.reconcile(req.trueBalance());
    }

    @PostMapping("/import")
    public ImportResult importStatement(@RequestBody ImportRequest req) {
        return money.importStatement(req.rows(), req.closingBalance());
    }
}
