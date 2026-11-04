package com.ena.mam.staffmember;

import jakarta.persistence.*;

@Entity
@Table(name = "staff_member")
public class StaffMember {

    public StaffMember() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long staffMemberId;

    @Column(name = "member_name", nullable = false, unique = true)
    private String staffMemberName;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public Long getStaffMemberId() {
        return staffMemberId;
    }

    public void setStaffMemberId(Long staffMemberId) {
        this.staffMemberId = staffMemberId;
    }

    public String getStaffMemberName() {
        return staffMemberName;
    }

    public void setStaffMemberName(String staffMemberName) {
        this.staffMemberName = staffMemberName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}