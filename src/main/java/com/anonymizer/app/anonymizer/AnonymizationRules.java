package com.anonymizer.app.anonymizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Factory class for creating anonymization rules
 */
public class AnonymizationRules {
    private static final Logger logger = LoggerFactory.getLogger(AnonymizationRules.class);
    
    // Rule types
    public static final String BANK_CARD = "BANK_CARD";
    public static final String ID_CARD = "ID_CARD";
    public static final String NAME = "NAME";
    public static final String MOBILE = "MOBILE";
    public static final String PHONE = "PHONE";
    public static final String EMAIL = "EMAIL";
    public static final String AMOUNT = "AMOUNT";
    public static final String TEXT = "TEXT";
    
    private static final Map<String, AnonymizationRule> rules = new HashMap<>();
    private static final Random random = new Random();
    
    static {
        // Initialize rules
        rules.put(BANK_CARD, new BankCardRule());
        rules.put(ID_CARD, new IdCardRule());
        rules.put(NAME, new NameRule());
        rules.put(MOBILE, new MobileRule());
        rules.put(PHONE, new PhoneRule());
        rules.put(EMAIL, new EmailRule());
        rules.put(AMOUNT, new AmountRule());
        rules.put(TEXT, new TextRule());
    }
    
    /**
     * Get the anonymization rule for the given type
     * 
     * @param type The rule type
     * @return The anonymization rule
     */
    public static AnonymizationRule getRule(String type) {
        AnonymizationRule rule = rules.get(type);
        if (rule == null) {
            logger.warn("No rule found for type: {}, using default rule", type);
            return new DefaultRule();
        }
        return rule;
    }
    
    /**
     * Generate a random digit
     */
    private static char getRandomDigit() {
        return (char) ('0' + random.nextInt(10));
    }
    
    /**
     * Generate a random lowercase letter
     */
    private static char getRandomLowercase() {
        return (char) ('a' + random.nextInt(26));
    }
    
    /**
     * Generate a random uppercase letter
     */
    private static char getRandomUppercase() {
        return (char) ('A' + random.nextInt(26));
    }
    
    /**
     * Generate a random letter (preserving case)
     */
    private static char getRandomLetter(char original) {
        if (Character.isUpperCase(original)) {
            return getRandomUppercase();
        } else {
            return getRandomLowercase();
        }
    }
    
    /**
     * Generate a random alphanumeric character (preserving type)
     */
    private static char getRandomAlphanumeric(char original) {
        if (Character.isDigit(original)) {
            return getRandomDigit();
        } else if (Character.isLetter(original)) {
            return getRandomLetter(original);
        } else {
            return original; // Keep special characters as is
        }
    }
    
    /**
     * Generate a random Chinese character
     */
    private static char getRandomChineseChar() {
        // Range of common Chinese characters in Unicode
        return (char) (0x4E00 + random.nextInt(0x9FA5 - 0x4E00));
    }
    
    /**
     * Generate a random Japanese character (Hiragana)
     */
    private static char getRandomJapaneseChar() {
        // Range of Hiragana characters in Unicode
        return (char) (0x3040 + random.nextInt(0x309F - 0x3040));
    }
    
    /**
     * Generate a random Korean character (Hangul)
     */
    private static char getRandomKoreanChar() {
        // Range of Hangul characters in Unicode
        return (char) (0xAC00 + random.nextInt(0xD7A3 - 0xAC00));
    }
    
    /**
     * Generate a random character of the same script as the original
     */
    private static char getRandomCharacterOfSameScript(char original) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(original);
        
        if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
            return getRandomChineseChar();
        } else if (block == Character.UnicodeBlock.HIRAGANA || block == Character.UnicodeBlock.KATAKANA) {
            return getRandomJapaneseChar();
        } else if (block == Character.UnicodeBlock.HANGUL_SYLLABLES) {
            return getRandomKoreanChar();
        } else if (Character.isLetter(original)) {
            // For Latin and other alphabets
            return getRandomLetter(original);
        } else if (Character.isDigit(original)) {
            return getRandomDigit();
        } else {
            // Keep other characters (punctuation, etc.) as is
            return original;
        }
    }
    
    /**
     * Generate a random amount value
     */
    private static String generateRandomAmount(String originalAmount) {
        try {
            // Try to parse the original amount to determine a reasonable range
            double original = Double.parseDouble(originalAmount.replaceAll("[^0-9.]", ""));
            
            // Generate a random amount with similar magnitude
            double magnitude = Math.pow(10, Math.floor(Math.log10(original)));
            double randomAmount = random.nextDouble() * magnitude * 10;
            
            // Format with same number of decimal places as original
            int decimalPlaces = 0;
            if (originalAmount.contains(".")) {
                decimalPlaces = originalAmount.length() - originalAmount.indexOf(".") - 1;
            }
            
            String format = "%." + decimalPlaces + "f";
            String result = String.format(format, randomAmount);
            
            // Preserve currency symbols or other non-numeric characters
            StringBuilder finalResult = new StringBuilder();
            int resultIndex = 0;
            
            for (char c : originalAmount.toCharArray()) {
                if (Character.isDigit(c) || c == '.') {
                    if (resultIndex < result.length()) {
                        finalResult.append(result.charAt(resultIndex++));
                    }
                } else {
                    finalResult.append(c); // Preserve currency symbols and other characters
                }
            }
            
            return finalResult.toString();
        } catch (NumberFormatException | ArithmeticException e) {
            // If parsing fails, generate a completely random amount
            return String.valueOf(random.nextInt(10000));
        }
    }
    
    /**
     * Generate random text preserving structure
     */
    private static String generateRandomText(String originalText) {
        if (originalText == null || originalText.isEmpty()) {
            return originalText;
        }
        
        StringBuilder result = new StringBuilder();
        String[] words = originalText.split("\\s+");
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            
            String word = words[i];
            StringBuilder randomWord = new StringBuilder();
            
            for (char c : word.toCharArray()) {
                randomWord.append(getRandomCharacterOfSameScript(c));
            }
            
            result.append(randomWord);
        }
        
        return result.toString();
    }
    
    /**
     * Default rule that returns the original value
     */
    private static class DefaultRule implements AnonymizationRule {
        @Override
        public String anonymize(String value) {
            return value;
        }
    }
    
    /**
     * Bank card rule: Replace all digits and letters with random ones, keeping the same length
     */
    private static class BankCardRule implements AnonymizationRule {
        @Override
        public String anonymize(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            
            StringBuilder anonymized = new StringBuilder();
            for (char c : value.toCharArray()) {
                anonymized.append(getRandomAlphanumeric(c));
            }
            
            return anonymized.toString();
        }
    }
    
    /**
     * ID card rule: Replace all digits and letters with random ones, keeping the same length
     */
    private static class IdCardRule implements AnonymizationRule {
        @Override
        public String anonymize(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            
            StringBuilder anonymized = new StringBuilder();
            for (char c : value.toCharArray()) {
                anonymized.append(getRandomAlphanumeric(c));
            }
            
            return anonymized.toString();
        }
    }
    
    /**
     * Name rule: Replace with random characters of the same script
     * - Chinese characters are replaced with random Chinese characters
     * - Japanese characters are replaced with random Japanese characters
     * - Korean characters are replaced with random Korean characters
     * - Latin alphabet characters are replaced with random Latin characters (preserving case)
     */
    private static class NameRule implements AnonymizationRule {
        @Override
        public String anonymize(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            
            StringBuilder anonymized = new StringBuilder();
            for (char c : value.toCharArray()) {
                anonymized.append(getRandomCharacterOfSameScript(c));
            }
            
            return anonymized.toString();
        }
    }
    
    /**
     * Mobile rule: Replace with random digits, keeping the same length
     */
    private static class MobileRule implements AnonymizationRule {
        @Override
        public String anonymize(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            
            StringBuilder anonymized = new StringBuilder();
            for (char c : value.toCharArray()) {
                if (Character.isDigit(c)) {
                    anonymized.append(getRandomDigit());
                } else {
                    anonymized.append(c); // Keep non-digit characters as is
                }
            }
            
            return anonymized.toString();
        }
    }
    
    /**
     * Phone rule: Replace with random digits, keeping the same length
     */
    private static class PhoneRule implements AnonymizationRule {
        @Override
        public String anonymize(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            
            StringBuilder anonymized = new StringBuilder();
            for (char c : value.toCharArray()) {
                if (Character.isDigit(c)) {
                    anonymized.append(getRandomDigit());
                } else {
                    anonymized.append(c); // Keep non-digit characters like '-' as is
                }
            }
            
            return anonymized.toString();
        }
    }
    
    /**
     * Email rule: Keep email format, replace letters with random letters
     */
    private static class EmailRule implements AnonymizationRule {
        @Override
        public String anonymize(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            
            StringBuilder anonymized = new StringBuilder();
            for (char c : value.toCharArray()) {
                if (Character.isLetter(c)) {
                    anonymized.append(getRandomLetter(c));
                } else {
                    anonymized.append(c); // Keep '@', '.', and other special characters
                }
            }
            
            return anonymized.toString();
        }
    }
    
    /**
     * Amount rule: Replace with random amount, not necessarily keeping the same length
     * Preserves currency symbols and formatting
     */
    private static class AmountRule implements AnonymizationRule {
        @Override
        public String anonymize(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            
            return generateRandomAmount(value);
        }
    }
    
    /**
     * Text rule: Replace text with random characters, preserving word structure
     */
    private static class TextRule implements AnonymizationRule {
        @Override
        public String anonymize(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            
            return generateRandomText(value);
        }
    }
} 