package br.com.estagio.anonymizer.core;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneAnonymizerTest {
    private static final String[] EXAMPLES = {
            "+556291064865",
            "556191213756",
            "+5511 972792222",
            "5511972792222",
            "+1 305 9023346",
            "+1609618-4620",
            "+54 9 3757 50-5105",
            "+591 72040940",
            "+351 963 830 852",
            "+595 984 563687",
            "+1 954 393-7920"
    };

    @Test
    void shouldAnonymizePlainPhone() {
        PhoneAnonymizer anonymizer = new PhoneAnonymizer();

        String result = anonymizer.anonymize("Telefone: 556191213756");
        String replacement = result.substring("Telefone: ".length());

        assertNotEquals("Telefone: 556191213756", result);
        assertEquals(12, replacement.length());
        assertTrue(replacement.matches("\\d{12}"));
    }

    @Test
    void shouldPreservePlusSpacesAndHyphens() {
        PhoneAnonymizer anonymizer = new PhoneAnonymizer();

        String original = "+1 954 393-7920";
        String result = anonymizer.anonymize(original);

        assertNotEquals(original, result);
        assertSameFormatting(original, result);
        assertEquals(countDigits(original), countDigits(result));
    }

    @Test
    void shouldKeepSameReplacementForRepeatedPhone() {
        PhoneAnonymizer anonymizer = new PhoneAnonymizer();

        String result = anonymizer.anonymize("A 556191213756 B 556191213756");
        String[] parts = result.split(" ");

        assertEquals(parts[1], parts[3]);
        assertNotEquals("556191213756", parts[1]);
    }

    @Test
    void shouldUseDifferentReplacementsForDifferentPhones() {
        PhoneAnonymizer anonymizer = new PhoneAnonymizer();

        String result = anonymizer.anonymize("A 556191213756 B 5511972792222");
        String[] parts = result.split(" ");

        assertNotEquals(parts[1], parts[3]);
        assertNotEquals("556191213756", parts[1]);
        assertNotEquals("5511972792222", parts[3]);
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
        Set<String> replacements = new HashSet<>();

        for (String example : EXAMPLES) {
            String result = anonymizer.anonymize(example);

            assertNotEquals(example, result);
            assertSameFormatting(example, result);
            assertEquals(countDigits(example), countDigits(result));
            assertFalse(replacements.contains(normalize(result)));
            replacements.add(normalize(result));
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
