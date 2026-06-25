package com.astrochakra.accounting.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.astrochakra.accounting.domain.Resource;
import com.astrochakra.accounting.repository.ResourceRepository;
import com.astrochakra.accounting.web.dto.ResourceRequest;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceRepository repo;

    public ResourceController(ResourceRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Resource> list() {
        return repo.findAllByOrderByCreatedAtAsc();
    }

    @PostMapping
    public Resource create(@RequestBody ResourceRequest r) {
        Resource res = new Resource();
        res.setId(UUID.randomUUID());
        apply(res, r);
        res.setIsActive(Boolean.TRUE);
        res.setCreatedAt(Instant.now());
        res.setUpdatedAt(Instant.now());
        return repo.save(res);
    }

    @PutMapping("/{id}")
    public Resource update(@PathVariable UUID id, @RequestBody ResourceRequest r) {
        Resource res = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
        apply(res, r);
        res.setUpdatedAt(Instant.now());
        return repo.save(res);
    }

    /** Soft delete (mirrors the existing app: is_active = false). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        repo.findById(id).ifPresent(res -> {
            res.setIsActive(Boolean.FALSE);
            res.setUpdatedAt(Instant.now());
            repo.save(res);
        });
    }

    private void apply(Resource res, ResourceRequest r) {
        res.setName(r.name());
        res.setRole(r.role() == null ? "" : r.role());
        res.setPayType(r.payType());
        res.setPayAmount(r.payAmount());
        res.setCommissionPercent(r.commissionPercent());
        res.setJobLabel(r.jobLabel() == null ? "per job" : r.jobLabel());
    }
}
