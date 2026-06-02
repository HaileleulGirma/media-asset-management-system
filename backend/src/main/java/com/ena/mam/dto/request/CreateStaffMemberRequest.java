package com.ena.mam.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateStaffMemberRequest(
        @NotEmpty(message = "A staff member's name cannot be empty.")
        @Pattern(
                regexp = "^[A-Z][a-z]+\\s[A-Z][a-z]+$",
                message = "A staff member's name must contain a first name and last name (e.g. John Smith)"
        )
        String staffMemberName,
        @NotNull(message = "A staff member's activity status cannot be empty.")
        Boolean isActive
) {
}
