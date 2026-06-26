package com.astrochakra.accounting.web;

import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.astrochakra.accounting.domain.Removal;
import com.astrochakra.accounting.repository.RemovalRepository;
import com.astrochakra.accounting.web.dto.RemovalRequest;

@RestController
@RequestMapping("/api/removals")
public class RemovalController {

    private final RemovalRepository repo;

    public RemovalController(RemovalRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Removal> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping
    public Removal create(@RequestBody RemovalRequest r) {
        Removal x = new Removal();
        x.setDate(r.date());
        x.setTime(r.time());
        x.setType(r.type());
        x.setLabel(r.label());
        x.setDetail(r.detail() == null ? "" : r.detail());
        x.setReason(r.reason() == null ? "" : r.reason());
        x.setCreatedAt(Instant.now());
        return repo.save(x);
    }
}
