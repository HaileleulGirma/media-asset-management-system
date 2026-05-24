package com.ena.mam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CameramanRepository extends JpaRepository<Cameraman, Long>{
    List <Cameraman> findByIsActiveTrue();
}
