package com.astrochakra.accounting.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.astrochakra.accounting.domain.SalaryPayment;
import com.astrochakra.accounting.service.SalaryService;
import com.astrochakra.accounting.web.dto.SalaryPaymentRequest;

@RestController
@RequestMapping("/api/salary-payments")
public class SalaryController {

    private final SalaryService service;

    public SalaryController(SalaryService service) {
        this.service = service;
    }

    @GetMapping
    public List<SalaryPayment> list() {
        return service.list();
    }

    /** Atomic: records the salary expense (tx + balance) and logs the payment. */
    @PostMapping
    public SalaryPayment pay(@RequestBody SalaryPaymentRequest req) {
        return service.pay(req);
    }
}
