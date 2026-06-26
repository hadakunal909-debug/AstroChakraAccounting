package com.astrochakra.accounting.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.astrochakra.accounting.domain.Transaction;
import com.astrochakra.accounting.repository.TransactionRepository;
import com.astrochakra.accounting.web.dto.CreateTransactionRequest;
import com.astrochakra.accounting.web.dto.TransactionMetaRequest;

@Service
public class TransactionService {

    private final TransactionRepository txs;

    public TransactionService(TransactionRepository txs) {
        this.txs = txs;
    }

    @Transactional(readOnly = true)
    public List<Transaction> list() {
        return txs.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Transaction create(CreateTransactionRequest r) {
        Transaction t = new Transaction();
        t.setDate(r.date());
        t.setProjectCode(blankToNull(r.projectCode()));
        t.setPerson(r.person());
        t.setAmount(r.amount());
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
        t.setCreatedAt(java.time.Instant.now());
        return txs.save(t);
    }

    @Transactional
    public List<Transaction> bulkCreate(List<CreateTransactionRequest> rows) {
        List<Transaction> out = new ArrayList<>();
        for (CreateTransactionRequest r : rows) {
            out.add(create(r));
        }
        return out;
    }

    @Transactional
    public Transaction settle(Long id) {
        Transaction t = find(id);
        t.setSettled(Boolean.TRUE);
        return txs.save(t);
    }

    /** Edit tags only — never amount/kind/fund_source. */
    @Transactional
    public Transaction updateMeta(Long id, TransactionMetaRequest r) {
        Transaction t = find(id);
        t.setDate(r.date());
        t.setPerson(r.person());
        t.setCategory(r.category() == null ? "" : r.category());
        t.setProjectCode(blankToNull(r.projectCode()));
        t.setNote(r.note() == null ? "" : r.note());
        return txs.save(t);
    }

    @Transactional
    public Transaction updateFundSource(Long id, String fundSource) {
        Transaction t = find(id);
        t.setFundSource(fundSource);
        return txs.save(t);
    }

    @Transactional
    public int renamePerson(String oldName, String newName) {
        return txs.renamePerson(oldName, newName);
    }

    private Transaction find(Long id) {
        return txs.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
