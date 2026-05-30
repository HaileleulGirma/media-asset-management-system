package com.ena.mam.dto.response;

public record CreateStaffMemberResponse(
        Long staffMemberId,
        String staffMemberName,
        Boolean isActive
) {
}
