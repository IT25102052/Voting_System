package com.slit.realityvote.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Validates that an end date/time field is after a start date/time field on the annotated class.
 */
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

    private String startFieldName;
    private String endFieldName;
    private boolean allowEqual;
    private String message;

    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.startFieldName = constraintAnnotation.startField();
        this.endFieldName = constraintAnnotation.endField();
        this.allowEqual = constraintAnnotation.allowEqual();
        this.message = constraintAnnotation.message();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            Object startVal = getPropertyValue(value, startFieldName);
            Object endVal = getPropertyValue(value, endFieldName);

            // If either value is null, defer to @NotNull constraints on the individual fields
            if (startVal == null || endVal == null) {
                return true;
            }

            if (!(startVal instanceof Comparable) || !(endVal instanceof Comparable)) {
                return false;
            }

            Comparable startComparable = (Comparable) startVal;
            Comparable endComparable = (Comparable) endVal;

            int cmp = endComparable.compareTo(startComparable);
            boolean valid = allowEqual ? cmp >= 0 : cmp > 0;

            if (!valid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode(endFieldName)
                        .addConstraintViolation();
                return false;
            }

            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Object getPropertyValue(Object target, String propertyName) {
        // Try Spring BeanWrapper first
        try {
            BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
            if (wrapper.isReadableProperty(propertyName)) {
                return wrapper.getPropertyValue(propertyName);
            }
        } catch (Exception ignored) {
        }

        // Fallback to getter method
        String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        try {
            Method method = target.getClass().getMethod(getterName);
            return method.invoke(target);
        } catch (Exception ignored) {
        }

        // Fallback to record accessor method
        try {
            Method method = target.getClass().getMethod(propertyName);
            return method.invoke(target);
        } catch (Exception ignored) {
        }

        // Fallback to direct field reflection
        try {
            Field field = target.getClass().getDeclaredField(propertyName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception ignored) {
        }

        return null;
    }
}
