package br.com.estagio.anonymizer.core;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlAnonymizerTest {
    private static final Pattern PLAIN_PHONE_PATTERN = Pattern.compile("\\b\\d{12}\\b");
    private static final Pattern TICKET_PATTERN = Pattern.compile("Internal Ticket Number:?\\s+(\\d+)");

    @Test
    void shouldAnonymizePhoneAndInternalTicketNumber() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize("Telefone 550000000000. Internal Ticket Number\t0000001");

        assertFalse(result.contains("550000000000"));
        assertFalse(result.contains("Internal Ticket Number\t0000001"));
        assertTrue(result.contains("Internal Ticket Number\t"));
        assertNotEquals("0000001", firstTicket(result));
    }

    @Test
    void shouldKeepSamePhoneReplacementInSameText() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize("A 550000000000 B 550000000000");
        Matcher matcher = PLAIN_PHONE_PATTERN.matcher(result);

        assertTrue(matcher.find());
        String first = matcher.group();
        assertTrue(matcher.find());
        assertEquals(first, matcher.group());
        assertNotEquals("550000000000", first);
    }

    @Test
    void shouldKeepSameTicketReplacementInSameText() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "Internal Ticket Number 0000001 e Internal Ticket Number: 0000001"
        );
        Matcher matcher = TICKET_PATTERN.matcher(result);

        assertTrue(matcher.find());
        String first = matcher.group(1);
        assertTrue(matcher.find());
        assertEquals(first, matcher.group(1));
        assertNotEquals("0000001", first);
    }

    @Test
    void shouldKeepConsistencyAcrossCallsWithSameInstance() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String first = anonymizer.anonymize("Telefone 550000000000. Internal Ticket Number 0000001");
        String second = anonymizer.anonymize("Contato 550000000000. Internal Ticket Number: 0000001");

        assertEquals(firstPhone(first), firstPhone(second));
        assertEquals(firstTicket(first), firstTicket(second));
    }

    @Test
    void shouldKeepTextWithoutPhoneOrTicketUnchanged() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String text = "Texto simples sem dados sensiveis.";

        assertEquals(text, anonymizer.anonymize(text));
    }

    @Test
    void shouldPreserveSimpleHtmlStructure() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "<td>Telefone 550000000000</td><td>Internal Ticket Number: 0000001</td>"
        );

        assertTrue(result.startsWith("<td>Telefone "));
        assertTrue(result.contains("</td><td>Internal Ticket Number: "));
        assertTrue(result.endsWith("</td>"));
        assertFalse(result.contains("550000000000"));
        assertNotEquals("0000001", firstTicket(result));
    }

    @Test
    void shouldNotAnonymizeInternalTicketNumberAsPhone() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize("Internal Ticket Number 00000001");

        assertEquals("00000002", firstTicket(result));
    }

    @Test
    void shouldAnonymizeSocialDivFieldsPhonesAndTableFieldsTogether() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "<div>Target</div><div>0000000000</div>"
                        + "<div>Email</div><div>conta.ficticia@example.com</div>"
                        + "<table><tr><th>Description</th><td>Texto sensivel de exemplo<br /></td></tr></table>"
                        + "<p>Telefone 550000000000</p>"
        );

        assertFalse(result.contains("<div>0000000000</div>"));
        assertTrue(result.contains("<div>Target</div><div>0000000001</div>"));
        assertFalse(result.contains("conta.ficticia@example.com"));
        assertFalse(result.contains("Texto sensivel de exemplo"));
        assertFalse(result.contains("550000000000"));
        assertTrue(result.contains("<div>Target</div>"));
        assertTrue(result.contains("<div>Email</div>"));
        assertTrue(result.contains("<table>"));
        assertTrue(result.contains("<th>Description</th><td>"));
        assertTrue(result.matches("(?s).*<p>Telefone \\d{12}</p>.*"));
    }

    private static String firstPhone(String value) {
        Matcher matcher = PLAIN_PHONE_PATTERN.matcher(value);
        assertTrue(matcher.find());
        return matcher.group();
    }

    private static String firstTicket(String value) {
        Matcher matcher = TICKET_PATTERN.matcher(value);
        assertTrue(matcher.find());
        return matcher.group(1);
    }

}
