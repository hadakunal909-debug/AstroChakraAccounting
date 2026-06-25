package com.astrochakra.accounting.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.astrochakra.accounting.domain.Balance;
import com.astrochakra.accounting.domain.Transaction;
import com.astrochakra.accounting.repository.BalanceRepository;
import com.astrochakra.accounting.repository.ProjectRepository;
import com.astrochakra.accounting.repository.TransactionRepository;
import com.astrochakra.accounting.web.dto.CreateTransactionRequest;
import com.astrochakra.accounting.web.dto.ImportResult;
import com.astrochakra.accounting.web.dto.ReconcileResult;

/**
 * Atomic money operations: each method does the full multi-step flow in ONE
 * database transaction (insert/update + balance + reserve + project allocation),
 * replacing the multi-call orchestration the browser does today.
 *
 * Balance model (current "Available = bank balance"): every expense reduces the
 * bank balance regardless of fund source; reserve/project allocations are tracked
 * for display only.
 */
@Service
public class MoneyService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final TransactionRepository txs;
    private final BalanceRepository balances;
    private final ProjectRepository projects;

    public MoneyService(TransactionRepository txs, BalanceRepository balances, ProjectRepository projects) {
        this.txs = txs;
        this.balances = balances;
        this.projects = projects;
    }

    /** Insert a transaction and apply its balance effect atomically. */
    @Transactional
    public Transaction record(CreateTransactionRequest r) {
        Transaction t = buildTx(r);
        txs.save(t);

        Balance b = loadOrCreateBalance();
        BigDecimal amt = nz(r.amount());
        String fund = t.getFundSource();

        if ("income".equals(t.getKind())) {
            b.setBalance(nz(b.getBalance()).add(amt));
        } else {
            // Every expense reduces the bank balance.
            b.setBalance(nz(b.getBalance()).subtract(amt));
            if ("reserve".equals(fund)) {
                b.setLiquidReserve(maxZero(nz(b.getLiquidReserve()).subtract(amt)));
            } else if (isProjectFund(fund)) {
                projects.findByCode(fund).ifPresent(p -> {
                    p.setAllocated(maxZero(nz(p.getAllocated()).subtract(amt)));
                    p.setUpdatedAt(Instant.now());
                    projects.save(p);
                });
            }
        }
        touch(b);
        return t;
    }

    /** Mark settled, write a JV reversal record, and restore the balance — atomically. */
    @Transactional
    public Transaction settle(UUID id, String note) {
        Transaction orig = txs.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        orig.setSettled(Boolean.TRUE);
        txs.save(orig);

        Transaction jv = new Transaction();
        jv.setId(UUID.randomUUID());
        jv.setDate(LocalDate.now().toString());
        jv.setProjectCode(orig.getProjectCode());
        jv.setPerson(orig.getPerson());
        jv.setAmount(orig.getAmount());
        jv.setKind("jv_reversal");
        jv.setCategory("JV");
        jv.setNote("REVERSAL: " + (note == null || note.isBlank() ? String.valueOf(orig.getAmount()) : note));
        jv.setSettled(Boolean.FALSE);
        jv.setIsReversal(Boolean.TRUE);
        jv.setOriginalId(orig.getId());
        jv.setFundSource(orig.getFundSource());
        jv.setCreatedAt(Instant.now());
        txs.save(jv);

        Balance b = loadOrCreateBalance();
        BigDecimal amt = nz(orig.getAmount());
        String fund = orig.getFundSource();
        if ("income".equals(orig.getKind())) {
            b.setBalance(nz(b.getBalance()).subtract(amt));
        } else if ("reserve".equals(fund)) {
            b.setBalance(nz(b.getBalance()).add(amt));
            b.setLiquidReserve(nz(b.getLiquidReserve()).add(amt));
        } else if (isProjectFund(fund)) {
            b.setBalance(nz(b.getBalance()).add(amt));
            projects.findByCode(fund).ifPresent(p -> {
                p.setAllocated(nz(p.getAllocated()).add(amt));
                p.setUpdatedAt(Instant.now());
                projects.save(p);
            });
        } else {
            b.setBalance(nz(b.getBalance()).add(amt));
        }
        touch(b);
        return jv;
    }

    /** Normalize legacy reserve/project-fund tags, zero the reserve, set the true balance. */
    @Transactional
    public ReconcileResult reconcile(BigDecimal trueBalance) {
        List<Transaction> affected = txs.findAll().stream()
                .filter(t -> !Boolean.TRUE.equals(t.getSettled())
                        && !Boolean.TRUE.equals(t.getIsReversal())
                        && t.getFundSource() != null
                        && !"available".equals(t.getFundSource())
                        && !"income".equals(t.getKind())
                        && !"jv_reversal".equals(t.getKind()))
                .collect(Collectors.toList());
        affected.forEach(t -> t.setFundSource("available"));
        txs.saveAll(affected);

        Balance b = loadOrCreateBalance();
        BigDecimal delta = trueBalance.subtract(nz(b.getBalance()));
        if (delta.signum() != 0) {
            Transaction adj = new Transaction();
            adj.setId(UUID.randomUUID());
            adj.setDate(LocalDate.now().toString());
            adj.setPerson("");
            adj.setAmount(delta.abs());
            adj.setKind(delta.signum() > 0 ? "income" : "general_expense");
            adj.setCategory("Adjustment");
            adj.setNote("Bank reconciliation: set Total Balance to " + trueBalance.toPlainString());
            adj.setSettled(Boolean.FALSE);
            adj.setIsReversal(Boolean.FALSE);
            adj.setFundSource("available");
            adj.setCreatedAt(Instant.now());
            txs.save(adj);
        }
        b.setBalance(trueBalance);
        b.setLiquidReserve(ZERO);
        touch(b);
        return new ReconcileResult(affected.size(), delta, trueBalance);
    }

    /** Bulk-insert imported rows and set the balance (closing balance, or current + net). */
    @Transactional
    public ImportResult importStatement(List<CreateTransactionRequest> rows, BigDecimal closingBalance) {
        BigDecimal net = ZERO;
        int count = 0;
        for (CreateTransactionRequest r : rows) {
            Transaction t = buildTx(r);
            txs.save(t);
            count++;
            net = "income".equals(t.getKind()) ? net.add(nz(r.amount())) : net.subtract(nz(r.amount()));
        }
        Balance b = loadOrCreateBalance();
        BigDecimal newBal = (closingBalance != null) ? closingBalance : nz(b.getBalance()).add(net);
        b.setBalance(newBal);
        touch(b);
        return new ImportResult(count, newBal);
    }

    // ===== helpers =====

    private Transaction buildTx(CreateTransactionRequest r) {
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setDate(r.date());
        t.setProjectCode(blankToNull(r.projectCode()));
        t.setPerson(r.person());
        t.setAmount(nz(r.amount()));
        t.setKind(r.kind());
        t.setCategory(r.category() == null ? "" : r.category());
        t.setNote(r.note() == null ? "" : r.note());
        t.setBillUrl(r.billUrl());
        t.setBillName(r.billName());
        t.setSettled(Boolean.FALSE);
        t.setIsReversal(Boolean.TRUE.equals(r.isReversal()));
        t.setOriginalId(r.originalId());
        t.setReversesKind(r.reversesKind());
        t.setFundSource(r.fundSource() == null ? "available" : r.fundSource());
        t.setCreatedAt(Instant.now());
        return t;
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

    private void touch(Balance b) {
        b.setUpdatedAt(Instant.now());
        balances.save(b);
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

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
