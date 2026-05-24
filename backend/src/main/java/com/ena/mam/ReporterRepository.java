package com.ena.mam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReporterRepository extends JpaRepository<Reporter, Long> {
    List <Reporter> findByIsActiveTrue();
}
