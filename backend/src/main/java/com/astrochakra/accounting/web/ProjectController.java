package com.astrochakra.accounting.web;

import java.time.Instant;
import java.util.List;

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

import com.astrochakra.accounting.domain.Project;
import com.astrochakra.accounting.repository.ProjectRepository;
import com.astrochakra.accounting.web.dto.ProjectRequest;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository repo;

    public ProjectController(ProjectRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Project> list() {
        return repo.findAllByOrderByCreatedAtAsc();
    }

    @PostMapping
    public Project create(@RequestBody ProjectRequest r) {
        Project p = new Project();
        p.setCode(r.code());
        p.setName(r.name());
        p.setFixedBudget(r.fixedBudget());
        p.setAllocated(r.allocated());
        p.setColor(r.color() == null ? "#6366f1" : r.color());
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return repo.save(p);
    }

    @PutMapping("/{id}")
    public Project update(@PathVariable Long id, @RequestBody ProjectRequest r) {
        Project p = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        if (r.name() != null) p.setName(r.name());
        if (r.fixedBudget() != null) p.setFixedBudget(r.fixedBudget());
        if (r.allocated() != null) p.setAllocated(r.allocated());
        if (r.color() != null) p.setColor(r.color());
        p.setUpdatedAt(Instant.now());
        return repo.save(p);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repo.deleteById(id);
    }
}
