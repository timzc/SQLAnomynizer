package com.anonymizer.app.anonymizer;

/**
 * Interface for anonymization rules
 */
public interface AnonymizationRule {
    /**
     * Anonymize the given value according to the rule
     * 
     * @param value The original value to anonymize
     * @return The anonymized value
     */
    String anonymize(String value);
} 