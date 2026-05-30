package com.ena.mam.dto.request;

public record CreateStaffMemberRequest(
        String staffMemberName,
        Boolean isActive
) {
}
