package com.ena.mam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {
    List <StaffMember> findByIsActiveTrue();
}
