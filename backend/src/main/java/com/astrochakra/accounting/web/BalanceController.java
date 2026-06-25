package com.astrochakra.accounting.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.astrochakra.accounting.domain.Balance;
import com.astrochakra.accounting.service.BalanceService;
import com.astrochakra.accounting.web.dto.BalanceDto;
import com.astrochakra.accounting.web.dto.BalanceUpdateRequest;

@RestController
@RequestMapping("/api/balance")
public class BalanceController {

    private final BalanceService service;

    public BalanceController(BalanceService service) {
        this.service = service;
    }

    @GetMapping
    public BalanceDto getBalance() {
        Balance b = service.getBalance();
        return new BalanceDto(b.getBalance(), b.getLiquidReserve());
    }

    @PutMapping
    public BalanceDto updateBalance(@RequestBody BalanceUpdateRequest req) {
        Balance b = service.updateBalance(req.balance(), req.liquidReserve());
        return new BalanceDto(b.getBalance(), b.getLiquidReserve());
    }
}
