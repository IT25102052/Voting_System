package com.slit.realityvote.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cross-field validator ensuring that an end date/time is chronologically after a start date/time.
 */
@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ValidDateRange.List.class)
public @interface ValidDateRange {

    String message() default "End date/time must be strictly after start date/time";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Name of the start date/time field.
     */
    String startField() default "startDate";

    /**
     * Name of the end date/time field.
     */
    String endField() default "endDate";

    /**
     * Whether equal start and end timestamps are permitted (default false).
     */
    boolean allowEqual() default false;

    @Documented
    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        ValidDateRange[] value();
    }
}
