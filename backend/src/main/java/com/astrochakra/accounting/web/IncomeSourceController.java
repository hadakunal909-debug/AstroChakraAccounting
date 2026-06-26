package com.astrochakra.accounting.web;

import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.astrochakra.accounting.domain.IncomeSource;
import com.astrochakra.accounting.repository.IncomeSourceRepository;
import com.astrochakra.accounting.web.dto.NameRequest;

@RestController
@RequestMapping("/api/income-sources")
public class IncomeSourceController {

    private final IncomeSourceRepository repo;

    public IncomeSourceController(IncomeSourceRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<IncomeSource> list() {
        return repo.findAllByOrderByCreatedAtAsc();
    }

    @PostMapping
    public IncomeSource create(@RequestBody NameRequest r) {
        IncomeSource s = new IncomeSource();
        s.setName(r.name());
        s.setCreatedAt(Instant.now());
        return repo.save(s);
    }
}
