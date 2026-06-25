package com.astrochakra.accounting.web;

import java.time.Instant;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.astrochakra.accounting.domain.CompanyInfo;
import com.astrochakra.accounting.repository.CompanyInfoRepository;
import com.astrochakra.accounting.web.dto.CompanyInfoRequest;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    private final CompanyInfoRepository repo;

    public CompanyController(CompanyInfoRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public CompanyInfo get() {
        return repo.findAll().stream().findFirst().orElseGet(() -> {
            CompanyInfo c = new CompanyInfo();
            c.setName("Astrochakra");
            return c;
        });
    }

    @PutMapping
    public CompanyInfo update(@RequestBody CompanyInfoRequest r) {
        CompanyInfo c = repo.findAll().stream().findFirst().orElseGet(() -> {
            CompanyInfo n = new CompanyInfo();
            n.setId(UUID.randomUUID());
            return n;
        });
        c.setName(r.name() == null ? "" : r.name());
        c.setAddress(r.address());
        c.setGstin(r.gstin());
        c.setEmail(r.email());
        c.setPhone(r.phone());
        c.setUpdatedAt(Instant.now());
        return repo.save(c);
    }
}
