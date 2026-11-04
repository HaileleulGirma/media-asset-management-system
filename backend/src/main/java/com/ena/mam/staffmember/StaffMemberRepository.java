package com.ena.mam.staffmember;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {
    List <StaffMember> findByActiveTrue();
    List <StaffMember> findByActiveFalse();

    boolean existsByStaffMemberNameIgnoreCase(String staffMemberName);
    boolean existsByStaffMemberNameIgnoreCaseAndStaffMemberIdNot(String staffMemberName, Long staffMemberId);
}
