package com.astrochakra.accounting.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.astrochakra.accounting.domain.ActivityLog;
import com.astrochakra.accounting.repository.ActivityLogRepository;
import com.astrochakra.accounting.web.dto.ActivityLogRequest;

@RestController
@RequestMapping("/api/activity-log")
public class ActivityController {

    private final ActivityLogRepository repo;

    public ActivityController(ActivityLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<ActivityLog> list(@RequestParam(defaultValue = "10") int limit) {
        return repo.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.max(1, limit)));
    }

    @PostMapping
    public ActivityLog log(@RequestBody ActivityLogRequest r) {
        ActivityLog a = new ActivityLog();
        a.setId(UUID.randomUUID());
        a.setUserName(r.userName());
        a.setUserRole(r.userRole());
        a.setAction(r.action());
        a.setEntityType(r.entityType() == null ? "" : r.entityType());
        a.setEntityId(r.entityId());
        a.setDescription(r.description() == null ? "" : r.description());
        a.setCreatedAt(Instant.now());
        return repo.save(a);
    }
}
