package br.com.estagio.anonymizer.core;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableFieldAnonymizerTest {
    private static final Set<String> FOOTBALL_TEAMS = Set.of(
            "Flamengo",
            "Palmeiras",
            "Corinthians",
            "Sao Paulo",
            "Santos",
            "Vasco",
            "Gremio",
            "Internacional",
            "Real Madrid",
            "Barcelona",
            "Atletico de Madrid",
            "Manchester United",
            "Manchester City",
            "Liverpool",
            "Chelsea",
            "Arsenal",
            "Bayern Munich",
            "Borussia Dortmund",
            "Paris Saint-Germain",
            "Juventus",
            "Milan",
            "Inter Milan",
            "Napoli",
            "Benfica",
            "Porto",
            "Ajax",
            "Boca Juniors",
            "River Plate",
            "Penarol",
            "Nacional",
            "Al Ahly",
            "Zamalek",
            "Galatasaray",
            "Fenerbahce",
            "Celtic",
            "Rangers"
    );

    @Test
    void shouldKeepOldInternalTicketNumberFormatWorking() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize("Internal Ticket Number 0000001");

        assertFalse(result.contains("Internal Ticket Number 0000001"));
        assertTrue(result.matches("Internal Ticket Number \\d{7}"));
    }

    @Test
    void shouldAnonymizeInternalTicketNumberInsideTableRow() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "<tr><th>Internal Ticket Number</th><td>0000001<br /></td></tr>"
        );
        String replacement = tdValue(result, "Internal Ticket Number");

        assertNotEquals("0000001", replacement);
        assertTrue(replacement.matches("\\d{7}"));
        assertTrue(result.contains("<tr><th>Internal Ticket Number</th><td>"));
        assertTrue(result.contains("<br /></td></tr>"));
    }

    @Test
    void shouldKeepSameTicketReplacementAcrossTableRows() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "<tr><th>Internal Ticket Number</th><td>0000001<br /></td></tr>"
                        + "<tr><th>Internal Ticket Number</th><td>0000001<br /></td></tr>"
        );
        Matcher matcher = tableValuePattern("Internal Ticket Number").matcher(result);

        assertTrue(matcher.find());
        String first = matcher.group(1);
        assertTrue(matcher.find());
        assertEquals(first, matcher.group(1));
    }

    @Test
    void shouldKeepSameTicketReplacementAcrossTableAndOldFormat() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "<tr><th>Internal Ticket Number</th><td>0000001<br /></td></tr>"
                        + "Internal Ticket Number: 0000001"
        );
        String tableTicket = tdValue(result, "Internal Ticket Number");
        Matcher inlineTicket = Pattern.compile("Internal Ticket Number: (\\d{7})").matcher(result);

        assertTrue(inlineTicket.find());
        assertEquals(tableTicket, inlineTicket.group(1));
    }

    @Test
    void shouldAnonymizeTableDescriptionAndKeepTags() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "<tr><th>Description</th><td>Texto sensivel de exemplo<br /></td></tr>"
        );
        String replacement = tdValue(result, "Description");

        assertNotEquals("Texto sensivel de exemplo", replacement);
        assertTrue(replacement.matches("[A-Za-z0-9]+"));
        assertTrue(result.contains("<tr><th>Description</th><td>"));
        assertTrue(result.contains("<br /></td></tr>"));
    }

    @Test
    void shouldKeepSameDescriptionReplacementAcrossRows() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "<tr><th>Description</th><td>Texto sensivel de exemplo<br /></td></tr>"
                        + "<tr><th>Description</th><td>Texto sensivel de exemplo<br /></td></tr>"
        );
        Matcher matcher = tableValuePattern("Description").matcher(result);

        assertTrue(matcher.find());
        String first = matcher.group(1);
        assertTrue(matcher.find());
        assertEquals(first, matcher.group(1));
    }

    @Test
    void shouldAnonymizeTableSubjectWithFootballTeamAndKeepTags() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "<tr><th>Subject</th><td>Grupo de exemplo<br /></td></tr>"
        );
        String replacement = tdValue(result, "Subject");

        assertNotEquals("Grupo de exemplo", replacement);
        assertTrue(FOOTBALL_TEAMS.contains(replacement));
        assertTrue(result.contains("<tr><th>Subject</th><td>"));
        assertTrue(result.contains("<br /></td></tr>"));
    }

    @Test
    void shouldKeepSameSubjectReplacementAcrossRows() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "<tr><th>Subject</th><td>Grupo de exemplo<br /></td></tr>"
                        + "<tr><th>Subject</th><td>Grupo de exemplo<br /></td></tr>"
        );
        Matcher matcher = tableValuePattern("Subject").matcher(result);

        assertTrue(matcher.find());
        String first = matcher.group(1);
        assertTrue(matcher.find());
        assertEquals(first, matcher.group(1));
    }

    @Test
    void shouldHandleSpacesAndLineBreaksBetweenTableTags() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "<table>\n"
                        + "  <tr>\n"
                        + "    <th>Internal Ticket Number</th>\n"
                        + "    <td>0000001<br /></td>\n"
                        + "  </tr>\n"
                        + "</table>"
        );

        assertNotEquals("0000001", tdValue(result, "Internal Ticket Number"));
        assertTrue(result.contains("<table>"));
        assertTrue(result.contains("</table>"));
        assertTrue(result.contains("<br /></td>"));
    }

    @Test
    void shouldNotAnonymizeTableTicketAsPhone() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "<tr><th>Internal Ticket Number</th><td>00000001<br /></td></tr>"
        );

        assertEquals("00000002", tdValue(result, "Internal Ticket Number"));
    }

    @Test
    void shouldNotInterfereWithPhoneAnonymization() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "<tr><th>Subject</th><td>Grupo de exemplo<br /></td></tr>"
                        + "<p>Telefone 550000000000</p>"
        );

        assertFalse(result.contains("Grupo de exemplo"));
        assertFalse(result.contains("550000000000"));
        assertTrue(result.matches("(?s).*<p>Telefone \\d{12}</p>.*"));
    }

    private static String tdValue(String html, String header) {
        Matcher matcher = tableValuePattern(header).matcher(html);
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    private static Pattern tableValuePattern(String header) {
        return Pattern.compile(
                "(?is)<tr\\b[^>]*>.*?<th\\b[^>]*>\\s*"
                        + Pattern.quote(header)
                        + "\\s*</th\\s*>.*?<td\\b[^>]*>\\s*(.*?)\\s*<br\\s*/?>.*?</td\\s*>.*?</tr\\s*>"
        );
    }
}
