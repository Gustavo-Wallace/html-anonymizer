package br.com.estagio.anonymizer.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketAnonymizerTest {
    @Test
    void shouldAnonymizeTicket() {
        TicketAnonymizer anonymizer = new TicketAnonymizer();

        String result = anonymizer.anonymize("Internal Ticket Number\t0000001");
        String replacement = result.substring("Internal Ticket Number\t".length());

        assertNotEquals("Internal Ticket Number\t0000001", result);
        assertEquals(7, replacement.length());
        assertTrue(replacement.matches("\\d{7}"));
    }

    @Test
    void shouldKeepSameReplacementForRepeatedTicket() {
        TicketAnonymizer anonymizer = new TicketAnonymizer();

        String result = anonymizer.anonymize(
                "Internal Ticket Number 0000001 e Internal Ticket Number: 0000001"
        );
        String[] replacements = result.split("Internal Ticket Number[ :]+");

        assertEquals(replacements[1].substring(0, 7), replacements[2]);
    }

    @Test
    void shouldUseDifferentReplacementsForDifferentTickets() {
        TicketAnonymizer anonymizer = new TicketAnonymizer();

        String result = anonymizer.anonymize(
                "Internal Ticket Number 0000001 e Internal Ticket Number 0000002"
        );
        String[] replacements = result.split("Internal Ticket Number ");

        assertNotEquals(replacements[1].substring(0, 7), replacements[2]);
    }

    @Test
    void shouldPreserveTabSpaceAndColonSeparators() {
        TicketAnonymizer anonymizer = new TicketAnonymizer();

        assertSameFormat("Internal Ticket Number\t0000001", anonymizer.anonymize("Internal Ticket Number\t0000001"));
        assertSameFormat("Internal Ticket Number 0000002", anonymizer.anonymize("Internal Ticket Number 0000002"));
        assertSameFormat("Internal Ticket Number: 0000003", anonymizer.anonymize("Internal Ticket Number: 0000003"));
    }

    @Test
    void shouldKeepTextWithoutInternalTicketNumberUnchanged() {
        TicketAnonymizer anonymizer = new TicketAnonymizer();

        String text = "Ticket externo 0000001 sem marcador interno.";

        assertEquals(text, anonymizer.anonymize(text));
    }

    private static void assertSameFormat(String original, String result) {
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
}
