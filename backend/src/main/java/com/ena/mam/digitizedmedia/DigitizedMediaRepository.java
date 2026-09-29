package com.ena.mam.digitizedmedia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DigitizedMediaRepository extends
        JpaRepository<DigitizedMedia, Long>,
        JpaSpecificationExecutor<DigitizedMedia> {
}