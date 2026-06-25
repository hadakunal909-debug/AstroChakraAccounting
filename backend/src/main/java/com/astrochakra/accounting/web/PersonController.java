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

import com.astrochakra.accounting.domain.Person;
import com.astrochakra.accounting.repository.PersonRepository;
import com.astrochakra.accounting.web.dto.PersonRequest;

@RestController
@RequestMapping("/api/people")
public class PersonController {

    private final PersonRepository repo;

    public PersonController(PersonRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Person> list() {
        return repo.findAllByOrderByCreatedAtAsc();
    }

    @PostMapping
    public Person create(@RequestBody PersonRequest r) {
        Person p = new Person();
        p.setId(UUID.randomUUID());
        p.setName(r.name());
        p.setRole(r.role() == null ? "Member" : r.role());
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return repo.save(p);
    }

    @PutMapping("/{id}")
    public Person update(@PathVariable UUID id, @RequestBody PersonRequest r) {
        Person p = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));
        if (r.name() != null) p.setName(r.name());
        if (r.role() != null) p.setRole(r.role());
        p.setUpdatedAt(Instant.now());
        return repo.save(p);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        repo.deleteById(id);
    }
}
