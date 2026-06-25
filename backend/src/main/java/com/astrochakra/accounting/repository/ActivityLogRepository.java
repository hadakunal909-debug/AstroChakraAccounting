package com.astrochakra.accounting.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
    List<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
