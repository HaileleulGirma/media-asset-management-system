package com.ena.mam.staffmember;

import jakarta.persistence.*;

@Entity
@Table(name = "staff_member")
public class StaffMember {

    public StaffMember() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_member_id")
    private Long staffMemberId;

    @Column(name = "staff_member_name")
    private String staffMemberName;

    @Column(name = "is_active")
    private Boolean isActive;

    public Long getStaffMemberId() {
        return staffMemberId;
    }

    public String getStaffMemberName() {
        return staffMemberName;
    }

    public void setStaffMemberName(String memberName) {
        this.staffMemberName = memberName;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }
}