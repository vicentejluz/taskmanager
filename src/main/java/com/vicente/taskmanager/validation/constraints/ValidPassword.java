package com.vicente.taskmanager.validation.constraints;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.*;


@Documented
@Constraint(validatedBy = {})
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@ReportAsSingleViolation
@Pattern(regexp = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@$!%*?&#.])[0-9a-zA-Z@$!%*?&#.]{8,}$")
public @interface ValidPassword {
    String message() default "is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
