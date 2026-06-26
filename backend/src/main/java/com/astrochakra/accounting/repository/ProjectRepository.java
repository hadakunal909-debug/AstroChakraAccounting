package com.astrochakra.accounting.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOrderByCreatedAtAsc();
    Optional<Project> findByCode(String code);
}
