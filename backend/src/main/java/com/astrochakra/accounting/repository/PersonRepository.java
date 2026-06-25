package com.astrochakra.accounting.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.Person;

public interface PersonRepository extends JpaRepository<Person, UUID> {
    List<Person> findAllByOrderByCreatedAtAsc();
}
