package com.ena.mam.location;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findByIsAbroadTrue();
    List<Location> findByIsAbroadFalse();

    boolean existsByLocationNameIgnoreCase(String locationName);
    boolean existsByLocationNameIgnoreCaseAndLocationIdNot(String locationName, Long locationId);
}
