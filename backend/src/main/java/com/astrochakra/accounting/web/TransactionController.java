package com.astrochakra.accounting.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.astrochakra.accounting.domain.Transaction;
import com.astrochakra.accounting.service.TransactionService;
import com.astrochakra.accounting.web.dto.CreateTransactionRequest;
import com.astrochakra.accounting.web.dto.FundSourceRequest;
import com.astrochakra.accounting.web.dto.RenamePersonRequest;
import com.astrochakra.accounting.web.dto.TransactionMetaRequest;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Transaction> list() {
        return service.list();
    }

    @PostMapping
    public Transaction create(@RequestBody CreateTransactionRequest req) {
        return service.create(req);
    }

    @PostMapping("/bulk")
    public List<Transaction> bulk(@RequestBody List<CreateTransactionRequest> rows) {
        return service.bulkCreate(rows);
    }

    @PutMapping("/{id}/settle")
    public Transaction settle(@PathVariable Long id) {
        return service.settle(id);
    }

    @PatchMapping("/{id}")
    public Transaction updateMeta(@PathVariable Long id, @RequestBody TransactionMetaRequest req) {
        return service.updateMeta(id, req);
    }

    @PatchMapping("/{id}/fund-source")
    public Transaction updateFundSource(@PathVariable Long id, @RequestBody FundSourceRequest req) {
        return service.updateFundSource(id, req.fundSource());
    }

    @PostMapping("/rename-person")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void renamePerson(@RequestBody RenamePersonRequest req) {
        service.renamePerson(req.oldName(), req.newName());
    }
}
