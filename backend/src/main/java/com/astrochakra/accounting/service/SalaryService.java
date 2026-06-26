package com.astrochakra.accounting.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.astrochakra.accounting.domain.SalaryPayment;
import com.astrochakra.accounting.repository.SalaryPaymentRepository;
import com.astrochakra.accounting.web.dto.CreateTransactionRequest;
import com.astrochakra.accounting.web.dto.SalaryPaymentRequest;

@Service
public class SalaryService {

    private final SalaryPaymentRepository payments;
    private final MoneyService money;

    public SalaryService(SalaryPaymentRepository payments, MoneyService money) {
        this.payments = payments;
        this.money = money;
    }

    @Transactional(readOnly = true)
    public List<SalaryPayment> list() {
        return payments.findAllByOrderByCreatedAtDesc();
    }

    /** Records the salary expense (tx + balance effect) AND logs the payment — atomically. */
    @Transactional
    public SalaryPayment pay(SalaryPaymentRequest r) {
        String fund = r.fundSource() == null ? "available" : r.fundSource();
        boolean isProjectFund = !"available".equals(fund) && !"reserve".equals(fund);
        String note = r.note() == null ? "" : r.note();
        String txNote = "Salary: " + r.resourceName() + (note.isBlank() ? "" : " — " + note);

        CreateTransactionRequest tx = new CreateTransactionRequest(
                LocalDate.now(),
                isProjectFund ? fund : null,
                r.resourceName(),
                r.amount(),
                "project_expense",
                "Salary",
                txNote,
                null, null, false, null, null,
                fund);
        money.record(tx); // insert expense + apply balance/reserve/allocation effect

        SalaryPayment sp = new SalaryPayment();
        sp.setId(UUID.randomUUID());
        sp.setResourceId(r.resourceId());
        sp.setResourceName(r.resourceName());
        sp.setPayType(r.payType());
        sp.setAmount(r.amount());
        sp.setUnits(r.units());
        sp.setFundSource(fund);
        sp.setFundLabel(r.fundLabel());
        sp.setPeriod(r.period());
        sp.setNote(note);
        sp.setPaidBy(r.paidBy());
        sp.setCreatedAt(Instant.now());
        return payments.save(sp);
    }
}
