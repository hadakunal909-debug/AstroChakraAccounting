package com.astrochakra.accounting.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.astrochakra.accounting.domain.ExpenseCategory;
import com.astrochakra.accounting.repository.ExpenseCategoryRepository;
import com.astrochakra.accounting.web.dto.NameRequest;

@RestController
@RequestMapping("/api/expense-categories")
public class ExpenseCategoryController {

    private final ExpenseCategoryRepository repo;

    public ExpenseCategoryController(ExpenseCategoryRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<ExpenseCategory> list() {
        return repo.findAllByOrderByNameAsc();
    }

    @PostMapping
    public ExpenseCategory create(@RequestBody NameRequest r) {
        ExpenseCategory c = new ExpenseCategory();
        c.setId(UUID.randomUUID());
        c.setName(r.name());
        c.setCreatedAt(Instant.now());
        return repo.save(c);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        repo.deleteById(id);
    }
}
