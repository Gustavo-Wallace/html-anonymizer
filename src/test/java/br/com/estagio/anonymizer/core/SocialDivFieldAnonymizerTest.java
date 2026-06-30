package br.com.estagio.anonymizer.core;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;
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
    void shouldAnonymizeVanityNameWhenValueIsChildOfLabel() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">"
                        + "Vanity Name"
                        + "<div class=\"m\"><div>usuario_ficticio<div class=\"p\"></div></div></div>"
                        + "</div>"
        );
        String replacement = valueAfterStructuredLabel(result, "Vanity Name");

        assertNotEquals("usuario_ficticio", replacement);
        assertTrue(replacement.matches("profile_[a-z0-9]+"));
        assertTrue(result.contains("Vanity Name"));
        assertTrue(result.contains("<div class=\"p\"></div>"));
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
    void shouldAnonymizeAccountIdentifierWhenValueIsChildOfLabel() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">"
                        + "Account Identifier"
                        + "<div class=\"m\"><div>https://www.instagram.com/usuario_ficticio<div class=\"p\"></div></div></div>"
                        + "</div>"
        );
        String replacement = valueAfterStructuredLabel(result, "Account Identifier");

        assertNotEquals("https://www.instagram.com/usuario_ficticio", replacement);
        assertTrue(replacement.matches("https://www\\.instagram\\.com/profile_[a-z0-9]+"));
        assertTrue(result.contains("Account Identifier"));
        assertTrue(result.contains("<div class=\"p\"></div>"));
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
    void shouldAnonymizeFirstInsideNameWhenValueIsChildOfFirstLabel() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">"
                        + "Name"
                        + "<div class=\"m\"><div><div class=\"t o\"><div class=\"t i\">"
                        + "First"
                        + "<div class=\"m\"><div>NomeFicticio<div class=\"p\"></div></div></div>"
                        + "</div></div></div></div>"
                        + "</div>"
        );
        String replacement = valueAfterStructuredLabel(result, "First");

        assertNotEquals("NomeFicticio", replacement);
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
    void shouldAnonymizeLastWhenValueIsChildOfLabel() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">"
                        + "Last"
                        + "<div class=\"m\"><div>Exemplo<div class=\"p\"></div></div></div>"
                        + "</div>"
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
    void shouldAnonymizeFullNameInsideMiddleNameWhenValueIsChildOfFullNameLabel() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">"
                        + "Middle Name"
                        + "<div class=\"m\"><div><div class=\"t i\">"
                        + "Full Name"
                        + "<div class=\"m\"><div>Nome Completo Ficticio<div class=\"p\"></div></div></div>"
                        + "</div></div></div>"
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
    void shouldAnonymizeEmailWhenValueIsChildOfLabel() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">"
                        + "Email"
                        + "<div class=\"m\"><div>conta.ficticia@example.com<div class=\"p\"></div></div></div>"
                        + "</div>"
        );
        String replacement = valueAfterStructuredLabel(result, "Email");

        assertNotEquals("conta.ficticia@example.com", replacement);
        assertTrue(replacement.matches("user_[a-z0-9]+@example\\.com"));
        assertTrue(result.contains("Email"));
        assertTrue(result.contains("<div class=\"p\"></div>"));
    }

    @Test
    void shouldAnonymizeRegisteredEmailAddressesWhenValueIsChildOfLabelAndKeepVerifiedSuffix() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">"
                        + "Registered Email Addresses"
                        + "<div class=\"m\"><div>usuario.ficticio@example.com (Verified)<div class=\"p\"></div></div></div>"
                        + "</div>"
        );
        String replacement = valueAfterStructuredLabel(result, "Registered Email Addresses");

        assertNotEquals("usuario.ficticio@example.com (Verified)", replacement);
        assertTrue(replacement.matches("user_[a-z0-9]+@example\\.com \\(Verified\\)"));
        assertTrue(result.contains("Registered Email Addresses"));
        assertTrue(result.contains("<div class=\"p\"></div>"));
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
    void shouldAnonymizeTargetWhenValueIsChildOfLabel() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">"
                        + "Target"
                        + "<div class=\"m\"><div>0000000000<div class=\"p\"></div></div></div>"
                        + "</div>"
        );
        String replacement = valueAfterStructuredLabel(result, "Target");

        assertNotEquals("0000000000", replacement);
        assertTrue(replacement.matches("\\d{10}"));
        assertTrue(result.contains("Target"));
        assertTrue(result.contains("<div class=\"p\"></div>"));
    }

    @Test
    void shouldAnonymizeInternalTicketNumberInDivAndKeepConsistencyWithInlineTicket() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">"
                        + "Internal Ticket Number"
                        + "<div class=\"m\"><div>0000001<div class=\"p\"></div></div></div>"
                        + "</div>"
                        + "Internal Ticket Number: 0000001"
        );
        String divTicket = valueAfterStructuredLabel(result, "Internal Ticket Number");
        Matcher inlineTicket = Pattern.compile("Internal Ticket Number: (\\d{7})").matcher(result);

        assertTrue(inlineTicket.find());
        assertEquals(divTicket, inlineTicket.group(1));
        assertNotEquals("0000001", divTicket);
        assertTrue(result.contains("Internal Ticket Number"));
        assertTrue(result.contains("<div class=\"p\"></div>"));
    }

    @Test
    void shouldAnonymizeDescriptionAndSubjectInDivs() {
        HtmlAnonymizer anonymizer = new HtmlAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">Description<div class=\"m\"><div>Texto sensivel de exemplo<div class=\"p\"></div></div></div></div>"
                        + "<div class=\"t i\">Subject<div class=\"m\"><div>Grupo de exemplo<div class=\"p\"></div></div></div></div>"
        );
        String description = valueAfterStructuredLabel(result, "Description");
        String subject = valueAfterStructuredLabel(result, "Subject");

        assertNotEquals("Texto sensivel de exemplo", description);
        assertTrue(description.matches("[A-Za-z0-9]+"));
        assertNotEquals("Grupo de exemplo", subject);
        assertTrue(subject.matches("[A-Za-z -]+"));
        assertTrue(result.contains("Description"));
        assertTrue(result.contains("Subject"));
        assertTrue(result.contains("<div class=\"p\"></div>"));
    }

    @Test
    void shouldAnonymizeNameLabelVariations() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">first name:<div class=\"m\"><div>Ana Exemplo<div class=\"p\"></div></div></div></div>"
                        + "<div class=\"t i\">last name:<div class=\"m\"><div>Sobrenome Exemplo<div class=\"p\"></div></div></div></div>"
                        + "<div class=\"t i\">middle name:<div class=\"m\"><div>Nome Meio<div class=\"p\"></div></div></div></div>"
                        + "<div class=\"t i\">Full Name<div class=\"m\"><div>Nome Completo Ficticio<div class=\"p\"></div></div></div></div>"
        );

        assertNotEquals("Ana Exemplo", valueAfterStructuredLabel(result, "First Name"));
        assertNotEquals("Sobrenome Exemplo", valueAfterStructuredLabel(result, "Last Name"));
        assertNotEquals("Nome Meio", valueAfterStructuredLabel(result, "Middle Name"));
        assertNotEquals("Nome Completo Ficticio", valueAfterStructuredLabel(result, "Full Name"));
        assertTrue(result.contains("first name:"));
        assertTrue(result.contains("last name:"));
        assertTrue(result.contains("middle name:"));
        assertTrue(result.contains("Full Name"));
        assertTrue(result.contains("<div class=\"p\"></div>"));
    }

    @Test
    void shouldAnonymizeEmailsDefinition() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">"
                        + "Emails Definition"
                        + "<div class=\"m\"><div>usuario.ficticio@example.com (Verified)<div class=\"p\"></div></div></div>"
                        + "</div>"
        );
        String replacement = valueAfterStructuredLabel(result, "Emails Definition");

        assertTrue(replacement.matches("user_[a-z0-9]+@example\\.com \\(Verified\\)"));
        assertTrue(result.contains("Emails Definition"));
        assertTrue(result.contains("<div class=\"p\"></div>"));
    }

    @Test
    void shouldAnonymizePortugueseEmailsDefinitionSubfield() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">"
                        + "Emails Definição"
                        + "<div class=\"m\"><div>E-mails: email.ficticio@example.com<div class=\"p\"></div></div></div>"
                        + "</div>"
        );
        String replacement = valueAfterStructuredLabel(result, "Emails Definição");

        assertTrue(replacement.matches("E-mails: user_[a-z0-9]+@example\\.com"));
        assertFalse(result.contains("email.ficticio@example.com"));
        assertTrue(result.contains("Emails Definição"));
        assertTrue(result.contains("<div class=\"p\"></div>"));
    }

    @Test
    void shouldAnonymizePortugueseRegisteredEmailAddresses() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">Endereços de e-mail registrados</div>"
                        + "<div class=\"m\"><div>usuario.ficticio@example.com (Verified)</div></div>"
        );
        String replacement = valueAfterStructuredLabel(result, "Endereços de e-mail registrados");

        assertTrue(replacement.matches("user_[a-z0-9]+@example\\.com \\(Verified\\)"));
        assertFalse(result.contains("usuario.ficticio@example.com"));
        assertTrue(result.contains("Endereços de e-mail registrados"));
    }

    @Test
    void shouldKeepPortugueseRegisteredEmailAddressesNoResponsiveRecord() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">Endereços de e-mail registrados</div>"
                        + "<div class=\"m\"><div>Nenhum registro responsivo localizado</div></div>"
        );

        assertEquals(
                "Nenhum registro responsivo localizado",
                valueAfterStructuredLabel(result, "Endereços de e-mail registrados")
        );
    }

    @Test
    void shouldAnonymizePortugueseSmallBusinessDefinitionSubfields() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">"
                        + "Definição de Pequena Média Empresa"
                        + "<div class=\"m\">"
                        + "<div>Endereço: Rua Ficticia 123</div>"
                        + "<div>Email: contato.empresa@example.com</div>"
                        + "<div>Nome: Empresa Ficticia LTDA</div>"
                        + "</div>"
                        + "</div>"
        );
        String replacement = valueAfterStructuredLabel(result, "Definição de Pequena Média Empresa");

        assertTrue(replacement.contains("Endereço: Endereco Ficticio "));
        assertTrue(replacement.matches("(?s).*Email: user_[a-z0-9]+@example\\.com.*"));
        assertTrue(replacement.contains("Nome: Empresa Ficticia "));
        assertFalse(result.contains("Rua Ficticia 123"));
        assertFalse(result.contains("contato.empresa@example.com"));
        assertFalse(result.contains("Empresa Ficticia LTDA"));
        assertTrue(result.contains("Definição de Pequena Média Empresa"));
    }

    @Test
    void shouldKeepPortugueseSmallBusinessNoResponsiveRecord() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">Pequenas Médias Empresas</div>"
                        + "<div class=\"m\"><div>Nenhum registro responsivo localizado</div></div>"
        );

        assertEquals(
                "Nenhum registro responsivo localizado",
                valueAfterStructuredLabel(result, "Pequenas Médias Empresas")
        );
    }

    @Test
    void shouldRecognizePortugueseLabelsWithoutAccentsAndWithColon() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">Emails Definicao:<div class=\"m\"><div>E-mail: outro.ficticio@example.com</div></div></div>"
                        + "<div class=\"t i\">Pequenas Medias Empresas:<div class=\"m\"><div>Endereco: Rua Exemplo 456</div></div></div>"
        );
        String emailDefinition = valueAfterStructuredLabel(result, "Emails Definicao");
        String smallBusiness = valueAfterStructuredLabel(result, "Pequenas Medias Empresas");

        assertTrue(emailDefinition.matches("E-mail: user_[a-z0-9]+@example\\.com"));
        assertTrue(smallBusiness.contains("Endereco: Endereco Ficticio "));
        assertFalse(result.contains("outro.ficticio@example.com"));
        assertFalse(result.contains("Rua Exemplo 456"));
    }

    @Test
    void shouldAnonymizeProfileUrlAndProfileValues() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">Profile URL<div class=\"m\"><div>https://www.instagram.com/usuario_ficticio</div></div></div>"
                        + "<div class=\"t i\">Profile<div class=\"m\"><div>perfil_ficticio</div></div></div>"
        );
        String profileUrl = valueAfterStructuredLabel(result, "Profile URL");
        String profile = valueAfterStructuredLabel(result, "Profile");

        assertTrue(profileUrl.matches("https://www\\.instagram\\.com/profile_[a-z0-9]+"));
        assertTrue(profile.matches("profile_[a-z0-9]+"));
        assertTrue(result.contains("Profile URL"));
        assertTrue(result.contains("Profile"));
    }

    @Test
    void shouldAnonymizeLooseUsernameImmediatelyBeforeProfileUrl() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div>usuario.exemplo123</div>"
                        + "<div class=\"t i\">Profile URL</div>"
                        + "<div class=\"m\"><div>instagram.com.br/usuario.exemplo123<div class=\"p\"></div></div></div>"
        );
        String looseUsername = directDivText(result, 0);
        String profileUrl = valueAfterStructuredLabel(result, "Profile URL");
        String urlIdentifier = finalProfileIdentifier(profileUrl);

        assertTrue(looseUsername.matches("profile_[a-z0-9]+"));
        assertEquals(looseUsername, urlIdentifier);
        assertEquals("instagram.com.br/" + looseUsername, profileUrl);
        assertFalse(result.contains("usuario.exemplo123"));
        assertTrue(result.contains("<div class=\"p\"></div>"));
    }

    @Test
    void shouldAnonymizeLooseUsernameBeforeInlineProfileUrl() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div>usuario.exemplo123</div>"
                        + "<div class=\"t i\">Profile URL: instagram.com.br/usuario.exemplo123</div>"
        );
        String looseUsername = directDivText(result, 0);
        String profileUrlText = Jsoup.parseBodyFragment(result).selectFirst("div.t.i").ownText();
        Matcher matcher = Pattern.compile("Profile URL: (instagram\\.com\\.br/\\S+)").matcher(profileUrlText);

        assertTrue(matcher.find());
        assertEquals(looseUsername, finalProfileIdentifier(matcher.group(1)));
        assertFalse(result.contains("usuario.exemplo123"));
    }

    @Test
    void shouldAnonymizeLooseUsernameBeforeProfileUrlWithDifferentDomain() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div>usuario.exemplo123</div>"
                        + "<div class=\"t i\">Profile URL: https://social.example/perfil/usuario.exemplo123?ref=abc</div>"
        );
        String looseUsername = directDivText(result, 0);
        String profileUrlText = Jsoup.parseBodyFragment(result).selectFirst("div.t.i").ownText();
        Matcher matcher = Pattern.compile("Profile URL: (https://social\\.example/perfil/\\S+)").matcher(profileUrlText);

        assertTrue(matcher.find());
        assertEquals(looseUsername, finalProfileIdentifier(matcher.group(1)));
        assertTrue(matcher.group(1).startsWith("https://social.example/perfil/"));
        assertTrue(matcher.group(1).endsWith("?ref=abc"));
        assertFalse(result.contains("usuario.exemplo123"));
    }

    @Test
    void shouldAnonymizeProfileUrlWithTrailingSlash() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div>usuario.exemplo123</div>"
                        + "<div class=\"t i\">Profile URL</div>"
                        + "<div class=\"m\"><div>social.example/usuario.exemplo123/</div></div>"
        );
        String looseUsername = directDivText(result, 0);
        String profileUrl = valueAfterStructuredLabel(result, "Profile URL");

        assertEquals(looseUsername, finalProfileIdentifier(profileUrl));
        assertEquals("social.example/" + looseUsername + "/", profileUrl);
        assertFalse(result.contains("usuario.exemplo123"));
    }

    @Test
    void shouldAnonymizeProfileUrlUsernameQueryParameter() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div>usuario.exemplo123</div>"
                        + "<div class=\"t i\">Profile URL</div>"
                        + "<div class=\"m\"><div>https://social.example/profile?username=usuario.exemplo123</div></div>"
        );
        String looseUsername = directDivText(result, 0);
        String profileUrl = valueAfterStructuredLabel(result, "Profile URL");

        assertEquals(looseUsername, queryParameterValue(profileUrl, "username"));
        assertEquals("https://social.example/profile?username=" + looseUsername, profileUrl);
        assertFalse(result.contains("usuario.exemplo123"));
    }

    @Test
    void shouldNotAnonymizeUnrelatedLooseTextBeforeProfileUrl() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div>texto sem relacao</div>"
                        + "<div class=\"t i\">Profile URL</div>"
                        + "<div class=\"m\"><div>instagram.com.br/usuario.exemplo123</div></div>"
        );
        String profileUrl = valueAfterStructuredLabel(result, "Profile URL");

        assertEquals("texto sem relacao", directDivText(result, 0));
        assertTrue(profileUrl.matches("instagram\\.com\\.br/profile_[a-z0-9]+"));
    }

    @Test
    void shouldKeepProfileIdentifierConsistentAcrossProfileUrlAccountIdentifierAndVanityName() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<div class=\"t i\">Vanity Name<div class=\"m\"><div>usuario.exemplo123</div></div></div>"
                        + "<div class=\"t i\">Account Identifier<div class=\"m\"><div>https://www.instagram.com/usuario.exemplo123</div></div></div>"
                        + "<div class=\"t i\">Profile URL<div class=\"m\"><div>instagram.com.br/usuario.exemplo123</div></div></div>"
        );
        String vanityName = valueAfterStructuredLabel(result, "Vanity Name");
        String accountIdentifier = valueAfterStructuredLabel(result, "Account Identifier");
        String profileUrl = valueAfterStructuredLabel(result, "Profile URL");

        assertEquals(vanityName, finalProfileIdentifier(accountIdentifier));
        assertEquals(vanityName, finalProfileIdentifier(profileUrl));
        assertFalse(result.contains("usuario.exemplo123"));
    }

    @Test
    void shouldAnonymizeLooseUsernameBeforeNestedProfileUrl() {
        SocialDivFieldAnonymizer anonymizer = new SocialDivFieldAnonymizer();

        String result = anonymizer.anonymize(
                "<section>"
                        + "<div>usuario.exemplo123</div>"
                        + "<div class=\"bloco\">"
                        + "<div class=\"t i\">Profile URL"
                        + "<div class=\"m\"><div>instagram.com.br/usuario.exemplo123<div class=\"p\"></div></div></div>"
                        + "</div>"
                        + "</div>"
                        + "</section>"
        );
        String looseUsername = directDivText(result, 0);
        String profileUrl = valueAfterStructuredLabel(result, "Profile URL");

        assertEquals(looseUsername, finalProfileIdentifier(profileUrl));
        assertTrue(result.contains("<div class=\"p\"></div>"));
        assertTrue(result.contains("Profile URL"));
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
            if (!normalizedLabel(labelElement.ownText()).equalsIgnoreCase(normalizedLabel(label))) {
                continue;
            }

            Element valueElement = firstChildWithClass(labelElement, "m");
            if (valueElement == null) {
                valueElement = labelElement.nextElementSibling();
                while (valueElement != null && !valueElement.hasClass("m")) {
                    valueElement = valueElement.nextElementSibling();
                }
            }

            assertTrue(valueElement != null);
            return valueElement.text().trim();
        }

        throw new AssertionError("Label not found: " + label);
    }

    private static String directDivText(String html, int index) {
        return Jsoup.parseBodyFragment(html).body().children().select("div").get(index).ownText().trim();
    }

    private static String finalProfileIdentifier(String value) {
        int queryStart = value.indexOf('?');
        if (queryStart >= 0) {
            value = value.substring(0, queryStart);
        }

        int fragmentStart = value.indexOf('#');
        if (fragmentStart >= 0) {
            value = value.substring(0, fragmentStart);
        }

        while (value.endsWith("/") && value.length() > 1) {
            value = value.substring(0, value.length() - 1);
        }

        return value.substring(value.lastIndexOf('/') + 1);
    }

    private static String queryParameterValue(String value, String key) {
        Matcher matcher = Pattern.compile("[?&]" + Pattern.quote(key) + "=([^&#]+)").matcher(value);
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    private static String normalizedLabel(String label) {
        String withoutAccents = Normalizer.normalize(label, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutAccents.replaceAll("\\s*:+\\s*$", "").replaceAll("\\s+", " ").trim();
    }

    private static Element firstChildWithClass(Element element, String className) {
        for (Element child : element.children()) {
            if (child.hasClass(className)) {
                return child;
            }
        }

        return null;
    }
}
