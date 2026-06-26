package com.astrochakra.accounting.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.Person;

public interface PersonRepository extends JpaRepository<Person, Long> {
    List<Person> findAllByOrderByCreatedAtAsc();
}
