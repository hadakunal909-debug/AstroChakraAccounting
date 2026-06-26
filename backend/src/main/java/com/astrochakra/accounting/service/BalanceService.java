package com.astrochakra.accounting.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.astrochakra.accounting.domain.Balance;
import com.astrochakra.accounting.repository.BalanceRepository;

@Service
public class BalanceService {

    private final BalanceRepository repo;

    public BalanceService(BalanceRepository repo) {
        this.repo = repo;
    }

    /** The app keeps a single balance row. Returns a zeroed default if none exists yet. */
    @Transactional(readOnly = true)
    public Balance getBalance() {
        return repo.findAll().stream().findFirst().orElseGet(() -> {
            Balance b = new Balance();
            b.setBalance(BigDecimal.ZERO);
            b.setLiquidReserve(BigDecimal.ZERO);
            return b;
        });
    }

    /**
     * Partial update mirroring updateBalance(balance, liquid): a null argument
     * leaves that field unchanged. Creates the row if it does not exist.
     */
    @Transactional
    public Balance updateBalance(BigDecimal balance, BigDecimal liquidReserve) {
        Balance b = repo.findAll().stream().findFirst().orElse(null);
        if (b == null) {
            b = new Balance();
            b.setBalance(BigDecimal.ZERO);
            b.setLiquidReserve(BigDecimal.ZERO);
        }
        if (balance != null) b.setBalance(balance);
        if (liquidReserve != null) b.setLiquidReserve(liquidReserve);
        b.setUpdatedAt(Instant.now());
        return repo.save(b);
    }
}
