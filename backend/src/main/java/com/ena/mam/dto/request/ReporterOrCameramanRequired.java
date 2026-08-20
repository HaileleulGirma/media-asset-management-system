package com.ena.mam.dto.request;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReporterOrCameramanRequiredValidator.class)
public @interface ReporterOrCameramanRequired {
    String message() default "At least one reporter or cameraman must be assigned.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}