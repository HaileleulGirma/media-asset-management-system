package com.ena.mam.dto.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ReporterOrCameramanRequiredValidator
        implements ConstraintValidator<ReporterOrCameramanRequired, CreateNewsRequest> {

    @Override
    public boolean isValid(CreateNewsRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // let @NotNull (if any) handle null request separately
        }
        boolean hasReporters = request.reporterIds() != null && !request.reporterIds().isEmpty();
        boolean hasCameramen = request.cameramanIds() != null && !request.cameramanIds().isEmpty();
        return hasReporters || hasCameramen;
    }
}