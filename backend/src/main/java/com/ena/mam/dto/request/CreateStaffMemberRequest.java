package com.ena.mam.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateStaffMemberRequest(
        @NotEmpty(message = "A staff member's name cannot be empty.")
        @Pattern(
                regexp = "^[\\p{L}]+(?:[\\s'-][\\p{L}]+)+$",
                message = "A staff member's name must contain a first name and last name."
        )
        String staffMemberName,
        @NotNull(message = "A staff member's activity status cannot be empty.")
        Boolean isActive
) {
}
