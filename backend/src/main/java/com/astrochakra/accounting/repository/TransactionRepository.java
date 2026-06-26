package com.astrochakra.accounting.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.astrochakra.accounting.domain.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("update Transaction t set t.person = :newName where t.person = :oldName")
    int renamePerson(@Param("oldName") String oldName, @Param("newName") String newName);
}
