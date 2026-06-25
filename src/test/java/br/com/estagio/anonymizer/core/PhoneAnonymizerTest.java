package br.com.estagio.anonymizer.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneAnonymizerTest {
    private static final String[] EXAMPLES = {
            "+550000000000",
            "550000000000",
            "+5500 000000000",
            "5500000000000",
            "+0 000 0000000",
            "+0000000-0000",
            "+00 0 0000 00-0000",
            "+000 00000000",
            "+000 000 000 000",
            "+000 000 000000",
            "+0 000 000-0000"
    };

    @Test
    void shouldAnonymizePlainPhone() {
        PhoneAnonymizer anonymizer = new PhoneAnonymizer();

        String result = anonymizer.anonymize("Telefone: 550000000000");
        String replacement = result.substring("Telefone: ".length());

        assertNotEquals("Telefone: 550000000000", result);
        assertEquals(12, replacement.length());
        assertTrue(replacement.matches("\\d{12}"));
    }

    @Test
    void shouldPreservePlusSpacesAndHyphens() {
        PhoneAnonymizer anonymizer = new PhoneAnonymizer();

        String original = "+0 000 000-0000";
        String result = anonymizer.anonymize(original);

        assertNotEquals(original, result);
        assertSameFormatting(original, result);
        assertEquals(countDigits(original), countDigits(result));
    }

    @Test
    void shouldKeepSameReplacementForRepeatedPhone() {
        PhoneAnonymizer anonymizer = new PhoneAnonymizer();

        String result = anonymizer.anonymize("A 550000000000 B 550000000000");
        String[] parts = result.split(" ");

        assertEquals(parts[1], parts[3]);
        assertNotEquals("550000000000", parts[1]);
    }

    @Test
    void shouldUseDifferentReplacementsForDifferentPhones() {
        PhoneAnonymizer anonymizer = new PhoneAnonymizer();

        String result = anonymizer.anonymize("A 550000000000 B 551111111111");
        String[] parts = result.split(" ");

        assertNotEquals(parts[1], parts[3]);
        assertNotEquals("550000000000", parts[1]);
        assertNotEquals("551111111111", parts[3]);
    }

    @Test
    void shouldKeepTextWithoutPhoneUnchanged() {
        PhoneAnonymizer anonymizer = new PhoneAnonymizer();

        String text = "Pedido 12345 sem telefone.";

        assertEquals(text, anonymizer.anonymize(text));
    }

    @Test
    void shouldDetectAndAnonymizeAllExamples() {
        PhoneAnonymizer anonymizer = new PhoneAnonymizer();
        Map<String, String> replacementsByOriginalPhone = new HashMap<>();
        Map<String, String> originalPhonesByReplacement = new HashMap<>();

        for (String example : EXAMPLES) {
            String result = anonymizer.anonymize(example);
            String normalizedExample = normalize(example);
            String normalizedResult = normalize(result);

            assertNotEquals(example, result);
            assertSameFormatting(example, result);
            assertEquals(countDigits(example), countDigits(result));

            String existingReplacement = replacementsByOriginalPhone.putIfAbsent(normalizedExample, normalizedResult);
            if (existingReplacement != null) {
                assertEquals(existingReplacement, normalizedResult);
            } else {
                assertFalse(originalPhonesByReplacement.containsKey(normalizedResult));
                originalPhonesByReplacement.put(normalizedResult, normalizedExample);
            }
        }
    }

    private static void assertSameFormatting(String original, String result) {
        assertEquals(original.length(), result.length());

        for (int i = 0; i < original.length(); i++) {
            char originalChar = original.charAt(i);
            char resultChar = result.charAt(i);
            if (Character.isDigit(originalChar)) {
                assertTrue(Character.isDigit(resultChar));
            } else {
                assertEquals(originalChar, resultChar);
            }
        }
    }

    private static int countDigits(String value) {
        return normalize(value).length();
    }

    private static String normalize(String value) {
        StringBuilder digits = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isDigit(current)) {
                digits.append(current);
            }
        }

        return digits.toString();
    }
}
