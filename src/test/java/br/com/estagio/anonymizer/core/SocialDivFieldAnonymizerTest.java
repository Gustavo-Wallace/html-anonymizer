package br.com.estagio.anonymizer.core;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialDivFieldAnonymizerTest {
    @Test
    void shouldAnonymizeNumericTarget() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize("<div>Target</div><div>0000000000</div>");
        String replacement = valueAfterLabel(result, "Target");

        assertNotEquals("0000000000", replacement);
        assertTrue(replacement.matches("\\d{10}"));
        assertTrue(result.contains("<div>Target</div>"));
    }

    @Test
    void shouldAnonymizeAccountIdentifierKeepingUrlStructure() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div>Account Identifier</div><div>https://www.instagram.com/usuario_ficticio</div>"
        );
        String replacement = valueAfterLabel(result, "Account Identifier");

        assertNotEquals("https://www.instagram.com/usuario_ficticio", replacement);
        assertTrue(replacement.matches("https://www\\.instagram\\.com/profile_[a-z0-9]+"));
        assertTrue(result.contains("<div>Account Identifier</div>"));
    }

    @Test
    void shouldAnonymizeRegisteredEmailAddressesAndKeepVerifiedSuffix() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div>Registered Email Addresses</div><div>usuario.ficticio@example.com (Verified)</div>"
        );
        String replacement = valueAfterLabel(result, "Registered Email Addresses");

        assertNotEquals("usuario.ficticio@example.com (Verified)", replacement);
        assertTrue(replacement.matches("user_[a-z0-9]+@example\\.com \\(Verified\\)"));
        assertTrue(result.contains("<div>Registered Email Addresses</div>"));
    }

    @Test
    void shouldAnonymizeEmail() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize("<div>Email</div><div>conta.ficticia@example.com</div>");
        String replacement = valueAfterLabel(result, "Email");

        assertNotEquals("conta.ficticia@example.com", replacement);
        assertTrue(replacement.matches("user_[a-z0-9]+@example\\.com"));
    }

    @Test
    void shouldAnonymizeVanityName() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize("<div>Vanity Name</div><div>usuario_ficticio</div>");
        String replacement = valueAfterLabel(result, "Vanity Name");

        assertNotEquals("usuario_ficticio", replacement);
        assertTrue(replacement.matches("profile_[a-z0-9]+"));
    }

    @Test
    void shouldAnonymizeNestedVanityNameAndPreserveAuxiliaryDiv() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">Vanity Name</div>"
                        + "<div class=\"m\"><div>usuario_ficticio<div class=\"p\"></div></div></div>"
        );
        String replacement = valueAfterStructuredLabel(result, "Vanity Name");

        assertNotEquals("usuario_ficticio", replacement);
        assertTrue(replacement.matches("profile_[a-z0-9]+"));
        assertTrue(result.contains("<div class=\"p\"></div>"));
        assertTrue(result.contains("Vanity Name"));
    }

    @Test
    void shouldAnonymizeNestedAccountIdentifier() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">Account Identifier</div>"
                        + "<div class=\"m\"><div>https://www.instagram.com/usuario_ficticio</div></div>"
        );
        String replacement = valueAfterStructuredLabel(result, "Account Identifier");

        assertNotEquals("https://www.instagram.com/usuario_ficticio", replacement);
        assertTrue(replacement.matches("https://www\\.instagram\\.com/profile_[a-z0-9]+"));
        assertTrue(result.contains("Account Identifier"));
    }

    @Test
    void shouldAnonymizeNameFirstSequence() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div>Name</div><div>First</div><div>Joao Ficticio</div>"
        );
        String replacement = valueAfterSequence(result, "Name", "First");

        assertNotEquals("Joao Ficticio", replacement);
        assertTrue(replacement.matches("[A-Za-z]+"));
        assertTrue(result.contains("<div>Name</div>"));
        assertTrue(result.contains("<div>First</div>"));
    }

    @Test
    void shouldAnonymizeNestedNameFirstSequence() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">Name</div>"
                        + "<div class=\"m\">"
                        + "<div class=\"t i\">First</div>"
                        + "<div class=\"m\"><div>Joao Ficticio<div class=\"p\"></div></div></div>"
                        + "</div>"
        );
        String replacement = valueAfterStructuredLabel(result, "First");

        assertNotEquals("Joao Ficticio", replacement);
        assertTrue(replacement.matches("[A-Za-z]+"));
        assertTrue(result.contains("Name"));
        assertTrue(result.contains("First"));
        assertTrue(result.contains("<div class=\"p\"></div>"));
    }

    @Test
    void shouldAnonymizeNameFirstAndLastSequence() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div>Name</div><div>First</div><div>Ana</div><div>Last</div><div>Exemplo</div>"
        );
        String firstName = valueAfterSequence(result, "Name", "First");
        String lastName = valueAfterLabel(result, "Last");

        assertNotEquals("Ana", firstName);
        assertNotEquals("Exemplo", lastName);
        assertTrue(firstName.matches("[A-Za-z]+"));
        assertTrue(lastName.matches("[A-Za-z]+"));
        assertTrue(result.contains("<div>Last</div>"));
    }

    @Test
    void shouldAnonymizeNestedLast() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">Last</div>"
                        + "<div class=\"m\"><div>Exemplo<div class=\"p\"></div></div></div>"
        );
        String replacement = valueAfterStructuredLabel(result, "Last");

        assertNotEquals("Exemplo", replacement);
        assertTrue(replacement.matches("[A-Za-z]+"));
        assertTrue(result.contains("Last"));
        assertTrue(result.contains("<div class=\"p\"></div>"));
    }

    @Test
    void shouldAnonymizeMiddleNameFullNameSequence() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div>Middle Name</div><div>Full Name</div><div>Nome Completo Ficticio</div>"
        );
        String replacement = valueAfterSequence(result, "Middle Name", "Full Name");

        assertNotEquals("Nome Completo Ficticio", replacement);
        assertTrue(replacement.matches("[A-Za-z]+ [A-Za-z]+"));
        assertTrue(result.contains("<div>Middle Name</div>"));
        assertTrue(result.contains("<div>Full Name</div>"));
    }

    @Test
    void shouldAnonymizeNestedFullName() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">Middle Name</div>"
                        + "<div class=\"m\">"
                        + "<div class=\"t i\">Full Name</div>"
                        + "<div class=\"m\"><div>Nome Completo Ficticio<div class=\"p\"></div></div></div>"
                        + "</div>"
        );
        String replacement = valueAfterStructuredLabel(result, "Full Name");

        assertNotEquals("Nome Completo Ficticio", replacement);
        assertTrue(replacement.matches("[A-Za-z]+ [A-Za-z]+"));
        assertTrue(result.contains("Middle Name"));
        assertTrue(result.contains("Full Name"));
        assertTrue(result.contains("<div class=\"p\"></div>"));
    }

    @Test
    void shouldAnonymizeNestedEmailAndRegisteredEmailAddresses() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">Email</div>"
                        + "<div class=\"m\"><div>conta.ficticia@example.com</div></div>"
                        + "<div class=\"t i\">Registered Email Addresses</div>"
                        + "<div class=\"m\"><div>usuario.ficticio@example.com (Verified)</div></div>"
        );
        String email = valueAfterStructuredLabel(result, "Email");
        String registeredEmail = valueAfterStructuredLabel(result, "Registered Email Addresses");

        assertNotEquals("conta.ficticia@example.com", email);
        assertTrue(email.matches("user_[a-z0-9]+@example\\.com"));
        assertNotEquals("usuario.ficticio@example.com (Verified)", registeredEmail);
        assertTrue(registeredEmail.matches("user_[a-z0-9]+@example\\.com \\(Verified\\)"));
        assertTrue(result.contains("Email"));
        assertTrue(result.contains("Registered Email Addresses"));
    }

    @Test
    void shouldAnonymizeNumericTargetInStructuredSocialField() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">Target</div><div class=\"m\"><div>0000000000</div></div>"
        );
        String replacement = valueAfterStructuredLabel(result, "Target");

        assertEquals("0000000001", replacement);
        assertTrue(result.contains("Target"));
    }

    @Test
    void shouldKeepSameReplacementForRepeatedValue() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div>Email</div><div>conta.ficticia@example.com</div>"
                        + "<div>Email</div><div>conta.ficticia@example.com</div>"
        );
        Matcher matcher = Pattern.compile("(?is)<div>Email</div>\\s*<div>(.*?)</div>").matcher(result);

        assertTrue(matcher.find());
        String first = matcher.group(1);
        assertTrue(matcher.find());
        assertEquals(first, matcher.group(1));
    }

    @Test
    void shouldPreserveDivStructureAndLabels() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<section>\n"
                        + "  <div class=\"label\">Target</div>\n"
                        + "  <div class=\"value\">0000000000</div>\n"
                        + "  <div>Account Identifier</div>\n"
                        + "  <div>https://www.facebook.com/usuario_ficticio</div>\n"
                        + "  <div>Name</div><div>First</div><div>Joao</div>\n"
                        + "  <div>Last</div><div>Exemplo</div>\n"
                        + "  <div>Middle Name</div><div>Full Name</div><div>Nome Completo Ficticio</div>\n"
                        + "  <div>Email</div><div>conta.ficticia@example.com</div>\n"
                        + "  <div>Registered Email Addresses</div><div>usuario.ficticio@example.com (Verified)</div>\n"
                        + "  <div>Vanity Name</div><div>usuario_ficticio</div>\n"
                        + "</section>"
        );

        assertTrue(result.contains("<section>"));
        assertTrue(result.contains("</section>"));
        assertTrue(result.contains("<div class=\"label\">Target</div>"));
        assertTrue(result.contains("<div>Account Identifier</div>"));
        assertTrue(result.contains("<div>Name</div>"));
        assertTrue(result.contains("<div>First</div>"));
        assertTrue(result.contains("<div>Last</div>"));
        assertTrue(result.contains("<div>Middle Name</div>"));
        assertTrue(result.contains("<div>Full Name</div>"));
        assertTrue(result.contains("<div>Email</div>"));
        assertTrue(result.contains("<div>Registered Email Addresses</div>"));
        assertTrue(result.contains("<div>Vanity Name</div>"));
        assertFalse(result.contains("usuario_ficticio"));
        assertFalse(result.contains("conta.ficticia@example.com"));
    }

    @Test
    void shouldTolerateSpacesAndLineBreaksBetweenDivs() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div>\n"
                        + "  Target\n"
                        + "</div>\n"
                        + "\n"
                        + "<div>\n"
                        + "  0000000000\n"
                        + "</div>"
        );

        assertNotEquals("0000000000", valueAfterLabel(result, "Target"));
    }

    private static String valueAfterLabel(String html, String label) {
        Matcher matcher = Pattern.compile(
                "(?is)<div\\b[^>]*>\\s*"
                        + Pattern.quote(label)
                        + "\\s*</div\\s*>\\s*<div\\b[^>]*>(.*?)</div\\s*>"
        ).matcher(html);
        assertTrue(matcher.find());
        return matcher.group(1).trim();
    }

    private static String valueAfterSequence(String html, String firstLabel, String secondLabel) {
        Matcher matcher = Pattern.compile(
                "(?is)<div\\b[^>]*>\\s*"
                        + Pattern.quote(firstLabel)
                        + "\\s*</div\\s*>\\s*<div\\b[^>]*>\\s*"
                        + Pattern.quote(secondLabel)
                        + "\\s*</div\\s*>\\s*<div\\b[^>]*>(.*?)</div\\s*>"
        ).matcher(html);
        assertTrue(matcher.find());
        return matcher.group(1).trim();
    }

    private static String valueAfterStructuredLabel(String html, String label) {
        Document document = Jsoup.parseBodyFragment(html);
        for (Element labelElement : document.select("div")) {
            if (!labelElement.ownText().trim().equalsIgnoreCase(label)) {
                continue;
            }

            Element valueElement = labelElement.nextElementSibling();
            while (valueElement != null && !valueElement.hasClass("m")) {
                valueElement = valueElement.nextElementSibling();
            }

            assertTrue(valueElement != null);
            return valueElement.text().trim();
        }

        throw new AssertionError("Label not found: " + label);
    }
}
