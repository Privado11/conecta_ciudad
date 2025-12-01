package com.unimagdalena.conectaCiudad.enums;

/**
 * Validation error codes for DTO field validations.
 * These codes should be used by the frontend for internationalization (i18n).
 * 
 * Naming convention: FIELD_VALIDATION_TYPE
 * Example: EMAIL_REQUIRED, PASSWORD_MIN_LENGTH
 */
public enum ValidationErrorCode {
    
    // Generic validation errors
    FIELD_REQUIRED("FIELD_REQUIRED"),
    FIELD_INVALID("FIELD_INVALID"),
    
    // String validations
    STRING_TOO_SHORT("STRING_TOO_SHORT"),
    STRING_TOO_LONG("STRING_TOO_LONG"),
    STRING_PATTERN_MISMATCH("STRING_PATTERN_MISMATCH"),
    STRING_BLANK("STRING_BLANK"),
    
    // Number validations
    NUMBER_TOO_SMALL("NUMBER_TOO_SMALL"),
    NUMBER_TOO_LARGE("NUMBER_TOO_LARGE"),
    NUMBER_INVALID("NUMBER_INVALID"),
    
    // Email validations
    EMAIL_REQUIRED("EMAIL_REQUIRED"),
    EMAIL_INVALID_FORMAT("EMAIL_INVALID_FORMAT"),
    
    // Password validations
    PASSWORD_REQUIRED("PASSWORD_REQUIRED"),
    PASSWORD_TOO_SHORT("PASSWORD_TOO_SHORT"),
    PASSWORD_TOO_WEAK("PASSWORD_TOO_WEAK"),
    
    // Date validations
    DATE_REQUIRED("DATE_REQUIRED"),
    DATE_PAST("DATE_PAST"),
    DATE_FUTURE("DATE_FUTURE"),
    DATE_INVALID("DATE_INVALID"),
    
    // Collection validations
    COLLECTION_EMPTY("COLLECTION_EMPTY"),
    COLLECTION_TOO_SMALL("COLLECTION_TOO_SMALL"),
    COLLECTION_TOO_LARGE("COLLECTION_TOO_LARGE"),
    
    // Custom business validations
    NATIONAL_ID_INVALID("NATIONAL_ID_INVALID"),
    PHONE_INVALID("PHONE_INVALID"),
    BUDGET_INVALID("BUDGET_INVALID"),
    VOTING_DATES_INVALID("VOTING_DATES_INVALID"),
    PROJECT_DATES_INVALID("PROJECT_DATES_INVALID");
    
    private final String code;
    
    ValidationErrorCode(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    /**
     * Maps Jakarta validation annotation types to specific error codes.
     * This helps provide more specific error codes based on the validation constraint.
     */
    public static String mapFromConstraint(String constraintName, String fieldName) {
        return switch (constraintName) {
            case "NotNull", "NotBlank", "NotEmpty" -> FIELD_REQUIRED.getCode();
            case "Email" -> EMAIL_INVALID_FORMAT.getCode();
            case "Size" -> fieldName.toLowerCase().contains("password") 
                ? PASSWORD_TOO_SHORT.getCode() 
                : STRING_TOO_SHORT.getCode();
            case "Min" -> NUMBER_TOO_SMALL.getCode();
            case "Max" -> NUMBER_TOO_LARGE.getCode();
            case "DecimalMin" -> NUMBER_TOO_SMALL.getCode();
            case "DecimalMax" -> NUMBER_TOO_LARGE.getCode();
            case "Pattern" -> STRING_PATTERN_MISMATCH.getCode();
            case "Past" -> DATE_PAST.getCode();
            case "Future" -> DATE_FUTURE.getCode();
            default -> FIELD_INVALID.getCode();
        };
    }
}
