package com.astrochakra.accounting.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.astrochakra.accounting.domain.ChangeRequest;
import com.astrochakra.accounting.repository.ChangeRequestRepository;
import com.astrochakra.accounting.web.dto.ChangeRequestRequest;
import com.astrochakra.accounting.web.dto.ReviewRequest;

@RestController
@RequestMapping("/api/change-requests")
public class ChangeRequestController {

    private final ChangeRequestRepository repo;

    public ChangeRequestController(ChangeRequestRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<ChangeRequest> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/mine")
    public List<ChangeRequest> mine(@RequestParam String username) {
        return repo.findByRequestedByOrderByCreatedAtDesc(username);
    }

    @PostMapping
    public ChangeRequest create(@RequestBody ChangeRequestRequest r) {
        ChangeRequest c = new ChangeRequest();
        c.setId(UUID.randomUUID());
        c.setRequestedBy(r.requestedBy());
        c.setEntityType(r.entityType());
        c.setEntityId(r.entityId());
        c.setChangeType(r.changeType());
        c.setDescription(r.description());
        c.setStatus("pending");
        c.setCreatedAt(Instant.now());
        return repo.save(c);
    }

    @PutMapping("/{id}/review")
    public ChangeRequest review(@PathVariable UUID id, @RequestBody ReviewRequest r) {
        ChangeRequest c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Change request not found"));
        c.setStatus(r.status());
        c.setReviewedBy(r.reviewedBy());
        c.setReviewNote(r.note() == null ? "" : r.note());
        c.setReviewedAt(Instant.now());
        return repo.save(c);
    }
}
