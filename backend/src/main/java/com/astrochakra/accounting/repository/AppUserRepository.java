package com.astrochakra.accounting.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
