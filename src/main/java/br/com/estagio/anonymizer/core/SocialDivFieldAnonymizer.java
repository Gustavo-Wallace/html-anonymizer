package br.com.estagio.anonymizer.core;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SocialDivFieldAnonymizer {
    private static final Pattern INLINE_PROFILE_LOCATION = Pattern.compile("(?i)(Profile\\s+UR[LI]\\s*:\\s*)(\\S+)");
    private static final Pattern EMAIL_WITH_SUFFIX = Pattern.compile("^([^\\s()]+@[^\\s()]+)(.*)$");
    private static final Pattern SENSITIVE_SUBFIELD = Pattern.compile(
            "(?iu)(\\b(?:Endere(?:ç|c)o|E-?mails?|Email|Nome)\\s*:\\s*)"
                    + "(.*?)"
                    + "(?=(?:\\s+\\b(?:Endere(?:ç|c)o|E-?mails?|Email|Nome)\\s*:)|$)"
    );
    private static final Pattern HTML_DOCUMENT_PATTERN = Pattern.compile("(?is)<\\s*html\\b");
    private static final String[] SUPPORTED_LABELS = {
            "target",
            "internal ticket number",
            "description",
            "subject",
            "account identifier",
            "registered email addresses",
            "emails definition",
            "email",
            "vanity name",
            "profile url",
            "profile uri",
            "first",
            "first name",
            "last",
            "last name",
            "middle",
            "middle name",
            "full name",
            "emails definição",
            "emails definicao",
            "endereços de e-mail registrados",
            "enderecos de e-mail registrados",
            "definição de e-mails",
            "definicao de e-mails",
            "definição de pequena média empresa",
            "definicao de pequena media empresa",
            "pequenas médias empresas",
            "pequenas medias empresas"
    };
    private static final String DESCRIPTION_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String DESCRIPTION_DIGITS = "0123456789";

    private static final String[] FIRST_NAMES = {
            "Ana", "Bruno", "Carla", "Diego", "Maria", "Pedro", "Laura", "Rafael", "Julia", "Lucas"
    };
    private static final String[] LAST_NAMES = {
            "Silva", "Santos", "Oliveira", "Costa", "Souza", "Lima", "Pereira", "Almeida", "Rocha", "Mendes"
    };
    private static final String[] FOOTBALL_TEAMS = {
            "Flamengo", "Palmeiras", "Corinthians", "Sao Paulo", "Santos", "Vasco", "Gremio",
            "Internacional", "Real Madrid", "Barcelona", "Manchester United", "Liverpool",
            "Chelsea", "Arsenal", "Bayern Munich", "Juventus", "Benfica", "Porto"
    };

    private final TicketAnonymizer ticketAnonymizer;
    private final Map<String, String> targetReplacements = new HashMap<>();
    private final Map<String, String> profileReplacements = new HashMap<>();
    private final Map<String, String> emailReplacements = new HashMap<>();
    private final Map<String, String> descriptionReplacements = new HashMap<>();
    private final Map<String, String> subjectReplacements = new HashMap<>();
    private final Map<String, String> firstNameReplacements = new HashMap<>();
    private final Map<String, String> lastNameReplacements = new HashMap<>();
    private final Map<String, String> middleNameReplacements = new HashMap<>();
    private final Map<String, String> fullNameReplacements = new HashMap<>();
    private final Map<String, String> addressReplacements = new HashMap<>();
    private final Map<String, String> companyNameReplacements = new HashMap<>();

    private final Set<String> usedTargets = new HashSet<>();
    private final Set<String> usedProfiles = new HashSet<>();
    private final Set<String> usedEmails = new HashSet<>();
    private final Set<String> usedDescriptions = new HashSet<>();
    private final Set<String> usedFirstNames = new HashSet<>();
    private final Set<String> usedLastNames = new HashSet<>();
    private final Set<String> usedMiddleNames = new HashSet<>();
    private final Set<String> usedFullNames = new HashSet<>();
    private final Set<String> usedAddresses = new HashSet<>();
    private final Set<String> usedCompanyNames = new HashSet<>();

    private long nextTarget = 1;
    private int nextToken = 1;
    private int nextFirstName;
    private int nextLastName;
    private int nextMiddleName;
    private int nextTeam;

    SocialDivFieldAnonymizer() {
        this(new TicketAnonymizer());
    }

    SocialDivFieldAnonymizer(TicketAnonymizer ticketAnonymizer) {
        this.ticketAnonymizer = Objects.requireNonNull(ticketAnonymizer);
    }

    String anonymize(String input) {
        if (input == null) {
            return null;
        }

        if (!mayContainSupportedSocialField(input)) {
            return input;
        }

        boolean fullDocument = HTML_DOCUMENT_PATTERN.matcher(input).find();
        Document document = fullDocument ? Jsoup.parse(input) : Jsoup.parseBodyFragment(input);
        document.outputSettings().prettyPrint(false);

        for (Element label : document.select("div")) {
            anonymizeLabel(label);
        }
        recomposeFullNames(document);

        return fullDocument ? document.outerHtml() : document.body().html();
    }

    private boolean mayContainSupportedSocialField(String input) {
        String lowerCaseInput = input.toLowerCase(Locale.ROOT);
        if (!lowerCaseInput.contains("<div")) {
            return false;
        }

        for (String label : SUPPORTED_LABELS) {
            if (lowerCaseInput.contains(label)) {
                return true;
            }
        }

        return false;
    }

    private void anonymizeLabel(Element label) {
        if (anonymizeInlineProfileUrl(label)) {
            return;
        }

        FieldType fieldType = fieldTypeFor(label);
        if (fieldType == null) {
            return;
        }

        Element valueElement = findValueElement(label);
        if (valueElement == null) {
            return;
        }

        String original = valueElement.text().trim();
        if (original.isEmpty()) {
            return;
        }

        if (fieldType == FieldType.MIDDLE_NAME
                && (fieldTypeFor(valueElement) != null || containsPotentialFieldLabel(valueElement))) {
            return;
        }

        if (isNoResponsiveRecord(original) && fieldType.preservesNoResponsiveRecord()) {
            return;
        }

        if (fieldType == FieldType.EMAIL_SUBFIELD_BLOCK || fieldType == FieldType.SMALL_BUSINESS_BLOCK) {
            anonymizeSensitiveSubfields(valueElement);
            return;
        }

        String replacement = fieldType == FieldType.FULL_NAME
                ? fullNameReplacementFor(label, original)
                : replacementFor(fieldType, original);
        replaceFirstTextNode(valueElement, replacement);
        if (fieldType == FieldType.PROFILE_IDENTIFIER && isProfileLocationLabel(label)) {
            anonymizePreviousLooseProfileText(label, original, replacement);
        }
    }

    private boolean isProfileLocationLabel(Element label) {
        String labelText = normalizedLabel(label);
        return isLabel(labelText, "Profile URL") || isLabel(labelText, "Profile URI");
    }

    private boolean anonymizeInlineProfileUrl(Element label) {
        for (TextNode textNode : label.textNodes()) {
            Matcher matcher = INLINE_PROFILE_LOCATION.matcher(textNode.text());
            if (!matcher.find()) {
                continue;
            }

            String original = matcher.group(2);
            String replacement = createProfileReplacement(original);
            String updated = textNode.text().substring(0, matcher.start(2))
                    + replacement
                    + textNode.text().substring(matcher.end(2));
            textNode.text(updated);
            anonymizePreviousLooseProfileText(label, original, replacement);
            return true;
        }

        return false;
    }

    private FieldType fieldTypeFor(Element label) {
        String labelText = normalizedLabel(label);

        if (isLabel(labelText, "Target")) {
            return FieldType.TARGET;
        }

        if (isLabel(labelText, "Internal Ticket Number")) {
            return FieldType.INTERNAL_TICKET_NUMBER;
        }

        if (isLabel(labelText, "Description")) {
            return FieldType.DESCRIPTION;
        }

        if (isLabel(labelText, "Subject")) {
            return FieldType.SUBJECT;
        }

        if (isLabel(labelText, "Account Identifier")
                || isLabel(labelText, "Vanity Name")
                || isLabel(labelText, "Profile URL")
                || isLabel(labelText, "Profile URI")) {
            return FieldType.PROFILE_IDENTIFIER;
        }

        if (isLabel(labelText, "Registered Email Addresses")
                || isLabel(labelText, "Emails Definition")
                || isLabel(labelText, "Endereços de e-mail registrados")
                || isLabel(labelText, "Enderecos de e-mail registrados")
                || isLabel(labelText, "Email")) {
            return FieldType.EMAIL;
        }

        if (isLabel(labelText, "Emails Definição")
                || isLabel(labelText, "Emails Definicao")
                || isLabel(labelText, "Definição de E-mails")
                || isLabel(labelText, "Definicao de E-mails")) {
            return FieldType.EMAIL_SUBFIELD_BLOCK;
        }

        if (isLabel(labelText, "Definição de Pequena Média Empresa")
                || isLabel(labelText, "Definicao de Pequena Media Empresa")
                || isLabel(labelText, "Pequenas Médias Empresas")
                || isLabel(labelText, "Pequenas Medias Empresas")) {
            return FieldType.SMALL_BUSINESS_BLOCK;
        }

        if (isLabel(labelText, "First") || isLabel(labelText, "First Name")) {
            return FieldType.FIRST_NAME;
        }

        if (isLabel(labelText, "Last") || isLabel(labelText, "Last Name")) {
            return FieldType.LAST_NAME;
        }

        if (isLabel(labelText, "Middle") || isLabel(labelText, "Middle Name")) {
            return FieldType.MIDDLE_NAME;
        }

        if (isLabel(labelText, "Full Name")) {
            return FieldType.FULL_NAME;
        }

        return null;
    }

    private Element findValueElement(Element label) {
        Element structuredValue = findStructuredValueElement(label);
        if (structuredValue != null) {
            return structuredValue;
        }

        return findNextDivValueElement(label);
    }

    private Element findStructuredValueElement(Element label) {
        if (!label.hasClass("t") || !label.hasClass("i")) {
            return null;
        }

        Element childValue = firstChildWithClass(label, "m");
        if (childValue != null) {
            return childValue;
        }

        Element descendantValue = firstCloseDescendantWithClass(label, "m");
        if (descendantValue != null) {
            return descendantValue;
        }

        Element siblingValue = firstNextElementSiblingWithClass(label, "m");
        if (siblingValue != null) {
            return siblingValue;
        }

        Element siblingContainingValue = firstNextElementSiblingContainingClass(label, "m");
        if (siblingContainingValue != null) {
            return siblingContainingValue;
        }

        Element parent = label.parent();
        if (parent == null) {
            return null;
        }

        Element parentSiblingValue = firstNextElementSiblingWithClass(parent, "m");
        if (parentSiblingValue != null) {
            return parentSiblingValue;
        }

        boolean afterLabel = false;
        for (Element child : parent.children()) {
            if (child == label) {
                afterLabel = true;
                continue;
            }

            if (afterLabel && child.hasClass("m")) {
                return child;
            }
        }

        return null;
    }

    private Element firstNextElementSiblingContainingClass(Element element, String className) {
        Element sibling = element.nextElementSibling();
        while (sibling != null) {
            if (isPotentialFieldLabel(sibling)) {
                return null;
            }

            Element descendant = sibling.selectFirst("div." + className);
            if (descendant != null) {
                return descendant;
            }

            sibling = sibling.nextElementSibling();
        }

        return null;
    }

    private Element firstChildWithClass(Element element, String className) {
        for (Element child : element.children()) {
            if (child.hasClass(className)) {
                return child;
            }
        }

        return null;
    }

    private Element firstCloseDescendantWithClass(Element element, String className) {
        for (Element child : element.children()) {
            if (isPotentialFieldLabel(child)) {
                continue;
            }

            if (child.hasClass(className)) {
                return child;
            }

            Element descendant = firstCloseDescendantWithClass(child, className);
            if (descendant != null) {
                return descendant;
            }
        }

        return null;
    }

    private Element firstNextElementSiblingWithClass(Element element, String className) {
        Element sibling = element.nextElementSibling();
        while (sibling != null) {
            if (sibling.hasClass(className)) {
                return sibling;
            }

            if (isPotentialFieldLabel(sibling)) {
                return null;
            }

            sibling = sibling.nextElementSibling();
        }

        return null;
    }

    private Element findNextDivValueElement(Element label) {
        Element next = label.nextElementSibling();
        while (next != null) {
            if ("div".equalsIgnoreCase(next.tagName())) {
                return next;
            }

            next = next.nextElementSibling();
        }

        return null;
    }

    private boolean isPotentialFieldLabel(Element element) {
        return element.hasClass("t") && element.hasClass("i") && fieldTypeFor(element) != null;
    }

    private boolean containsPotentialFieldLabel(Element element) {
        for (Element child : element.select("div")) {
            if (child != element && isPotentialFieldLabel(child)) {
                return true;
            }
        }

        return false;
    }

    private boolean replaceFirstTextNode(Element valueElement, String replacement) {
        for (TextNode textNode : valueElement.textNodes()) {
            if (!textNode.text().trim().isEmpty()) {
                textNode.text(replacement);
                return true;
            }
        }

        for (Node child : valueElement.childNodes()) {
            if (child instanceof Element childElement && replaceFirstTextNode(childElement, replacement)) {
                return true;
            }
        }

        valueElement.text(replacement);
        return true;
    }

    private String replacementFor(FieldType fieldType, String original) {
        return switch (fieldType) {
            case TARGET -> targetReplacements.computeIfAbsent(original, this::createTargetReplacement);
            case INTERNAL_TICKET_NUMBER -> createTicketReplacement(original);
            case DESCRIPTION -> descriptionReplacements.computeIfAbsent(original, this::createDescriptionReplacement);
            case SUBJECT -> subjectReplacements.computeIfAbsent(original, ignored -> nextFootballTeam());
            case PROFILE_IDENTIFIER -> createProfileReplacement(original);
            case EMAIL -> emailReplacements.computeIfAbsent(original, this::createEmailReplacement);
            case FIRST_NAME -> firstNameReplacements.computeIfAbsent(original, this::createFirstNameReplacement);
            case LAST_NAME -> lastNameReplacements.computeIfAbsent(original, this::createLastNameReplacement);
            case MIDDLE_NAME -> middleNameReplacements.computeIfAbsent(original, this::createMiddleNameReplacement);
            case FULL_NAME -> fullNameReplacements.computeIfAbsent(original, this::createFullNameReplacement);
            case EMAIL_SUBFIELD_BLOCK, SMALL_BUSINESS_BLOCK -> original;
        };
    }

    private String normalizedLabel(Element element) {
        String ownText = element.ownText().replaceAll("\\s+", " ").trim();
        String text = ownText;
        if (text.isEmpty() && element.hasClass("t") && element.hasClass("i")) {
            text = element.text().replaceAll("\\s+", " ").trim();
        }

        return text.replaceAll("\\s*:+\\s*$", "").replaceAll("\\s+", " ").trim();
    }

    private boolean isLabel(String value, String expected) {
        return normalizedKey(value).equals(normalizedKey(expected));
    }

    private String normalizedKey(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutAccents
                .replaceAll("\\s*:+\\s*$", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String createTargetReplacement(String original) {
        if (!original.matches("\\d+")) {
            return createVanityNameReplacement(original);
        }

        String replacement;
        do {
            replacement = zeroPad(nextTarget, original.length());
            nextTarget++;
        } while (replacement.equals(original) || usedTargets.contains(replacement));

        usedTargets.add(replacement);
        return replacement;
    }

    private String createTicketReplacement(String original) {
        Matcher matcher = Pattern.compile("\\d+").matcher(original);
        if (!matcher.find()) {
            return original;
        }

        String ticket = matcher.group();
        String replacement = ticketAnonymizer.anonymizeTicketValue(ticket);
        return original.substring(0, matcher.start()) + replacement + original.substring(matcher.end());
    }

    private String createProfileReplacement(String original) {
        ProfileValueParts parts = profileValueParts(original);
        String token = profileReplacements.computeIfAbsent(parts.identifier(), this::createProfileToken);
        return parts.base() + token + parts.suffix();
    }

    private String createProfileToken(String original) {
        String replacement;
        do {
            replacement = nextProfileToken();
        } while (replacement.equals(original) || usedProfiles.contains(replacement));

        usedProfiles.add(replacement);
        return replacement;
    }

    private ProfileValueParts profileValueParts(String original) {
        String value = original.trim();
        ProfileValueParts pathParts = profilePathValueParts(value);
        ProfileValueParts queryParts = profileQueryValueParts(value);
        if (pathParts != null && !isGenericProfilePathSegment(pathParts.identifier())) {
            return pathParts;
        }

        if (queryParts != null) {
            return queryParts;
        }

        if (pathParts != null) {
            return pathParts;
        }

        return new ProfileValueParts("", original, "");
    }

    private ProfileValueParts profilePathValueParts(String value) {
        int pathEnd = firstNonNegative(value.indexOf('?'), value.indexOf('#'), value.length());
        int firstPathSlash = firstPathSlash(value, pathEnd);
        if (firstPathSlash < 0) {
            return null;
        }

        int candidateEnd = pathEnd;
        while (candidateEnd > firstPathSlash && value.charAt(candidateEnd - 1) == '/') {
            candidateEnd--;
        }

        int candidateStart = value.lastIndexOf('/', candidateEnd - 1) + 1;
        if (candidateStart <= firstPathSlash || candidateStart >= candidateEnd) {
            return null;
        }

        String candidate = value.substring(candidateStart, candidateEnd);
        if (!isProfileIdentifierCandidate(candidate)) {
            return null;
        }

        return new ProfileValueParts(
                value.substring(0, candidateStart),
                candidate,
                value.substring(candidateEnd)
        );
    }

    private int firstPathSlash(String value, int pathEnd) {
        int schemeIndex = value.indexOf("://");
        int searchStart = schemeIndex >= 0 ? schemeIndex + 3 : 0;
        int firstSlash = value.indexOf('/', searchStart);
        if (firstSlash >= 0 && firstSlash < pathEnd) {
            return firstSlash;
        }

        return -1;
    }

    private ProfileValueParts profileQueryValueParts(String value) {
        int queryStart = value.indexOf('?');
        if (queryStart < 0) {
            return null;
        }

        int queryEnd = firstNonNegative(value.indexOf('#', queryStart + 1), value.length());
        int paramStart = queryStart + 1;
        while (paramStart < queryEnd) {
            int paramEnd = value.indexOf('&', paramStart);
            if (paramEnd < 0 || paramEnd > queryEnd) {
                paramEnd = queryEnd;
            }

            int equals = value.indexOf('=', paramStart);
            if (equals > paramStart && equals < paramEnd) {
                String key = value.substring(paramStart, equals).toLowerCase(Locale.ROOT);
                String candidate = value.substring(equals + 1, paramEnd);
                if (isProfileQueryKey(key) && isProfileIdentifierCandidate(candidate)) {
                    return new ProfileValueParts(
                            value.substring(0, equals + 1),
                            candidate,
                            value.substring(paramEnd)
                    );
                }
            }

            paramStart = paramEnd + 1;
        }

        return null;
    }

    private int firstNonNegative(int first, int second, int fallback) {
        if (first < 0) {
            return second < 0 ? fallback : second;
        }

        if (second < 0) {
            return first;
        }

        return Math.min(first, second);
    }

    private int firstNonNegative(int first, int fallback) {
        return first < 0 ? fallback : first;
    }

    private boolean isProfileQueryKey(String key) {
        return key.equals("username")
                || key.equals("user")
                || key.equals("handle")
                || key.equals("profile")
                || key.equals("profile_id")
                || key.equals("profileid")
                || key.equals("account")
                || key.equals("account_id")
                || key.equals("accountid")
                || key.equals("vanity")
                || key.equals("vanity_name")
                || key.equals("vanityname");
    }

    private boolean isGenericProfilePathSegment(String value) {
        String lowerCaseValue = value.toLowerCase(Locale.ROOT);
        return lowerCaseValue.equals("profile")
                || lowerCaseValue.equals("profiles")
                || lowerCaseValue.equals("perfil")
                || lowerCaseValue.equals("perfis")
                || lowerCaseValue.equals("user")
                || lowerCaseValue.equals("users")
                || lowerCaseValue.equals("account")
                || lowerCaseValue.equals("accounts")
                || lowerCaseValue.equals("page")
                || lowerCaseValue.equals("pages");
    }

    private boolean isProfileIdentifierCandidate(String value) {
        return !value.isBlank()
                && value.length() >= 3
                && !value.matches(".*[\\s/?&#=].*");
    }

    private void anonymizePreviousLooseProfileText(Element label, String original, String replacement) {
        ProfileValueParts originalParts = profileValueParts(original);
        ProfileValueParts replacementParts = profileValueParts(replacement);
        String originalIdentifier = originalParts.identifier();
        String replacementIdentifier = replacementParts.identifier();
        if (!looksLikeUsername(originalIdentifier)) {
            return;
        }

        Element current = label;
        while (current != null && !"body".equalsIgnoreCase(current.tagName())) {
            if (anonymizePreviousLooseProfileTextForNode(current, originalIdentifier, replacementIdentifier)) {
                return;
            }

            current = current.parent();
        }
    }

    private boolean anonymizePreviousLooseProfileTextForNode(
            Element element,
            String originalIdentifier,
            String replacementIdentifier
    ) {
        Node previous = element.previousSibling();
        while (previous != null) {
            if (previous instanceof TextNode textNode) {
                String text = textNode.text();
                String trimmed = text.trim();
                if (trimmed.isEmpty()) {
                    previous = previous.previousSibling();
                    continue;
                }

                if (trimmed.equals(originalIdentifier)) {
                    textNode.text(text.replace(originalIdentifier, replacementIdentifier));
                    return true;
                }

                return false;
            }

            if (previous instanceof Element previousElement) {
                String text = previousElement.text().trim();
                if (text.isEmpty()) {
                    previous = previous.previousSibling();
                    continue;
                }

                if (!isPotentialFieldLabel(previousElement) && text.equals(originalIdentifier)) {
                    replaceFirstTextNode(previousElement, replacementIdentifier);
                    return true;
                }

                return false;
            }

            previous = previous.previousSibling();
        }

        return false;
    }

    private boolean looksLikeUsername(String value) {
        return value.matches("(?i)^(?=.*[a-z])[a-z0-9._-]{3,}$");
    }

    private String createEmailReplacement(String original) {
        if (isNoResponsiveRecord(original)) {
            return original;
        }

        String replacement;
        do {
            Matcher matcher = EMAIL_WITH_SUFFIX.matcher(original);
            if (!matcher.matches()) {
                return original;
            }

            String suffix = matcher.matches() ? matcher.group(2) : "";
            replacement = "user_" + nextToken() + "@example.com" + suffix;
        } while (replacement.equals(original) || usedEmails.contains(replacement));

        usedEmails.add(replacement);
        return replacement;
    }

    private void anonymizeSensitiveSubfields(Element valueElement) {
        for (TextNode textNode : valueElement.textNodes()) {
            textNode.text(anonymizeSensitiveSubfields(textNode.text()));
        }

        for (Node child : valueElement.childNodes()) {
            if (child instanceof Element childElement) {
                anonymizeSensitiveSubfields(childElement);
            }
        }
    }

    private String anonymizeSensitiveSubfields(String text) {
        Matcher matcher = SENSITIVE_SUBFIELD.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String subfield = matcher.group(1);
            String value = matcher.group(2);
            String replacement = replacementForSubfield(subfield, value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(subfield + replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String replacementForSubfield(String subfield, String value) {
        Matcher matcher = Pattern.compile("^(\\s*)(.*?)(\\s*)$").matcher(value);
        if (!matcher.matches()) {
            return value;
        }

        String coreValue = matcher.group(2);
        if (coreValue.isBlank() || isNoResponsiveRecord(coreValue)) {
            return value;
        }

        String normalizedSubfield = normalizedKey(subfield.replace(":", ""));
        String replacement;
        if (normalizedSubfield.contains("mail")) {
            replacement = createEmailReplacement(coreValue);
        } else if (normalizedSubfield.equals("endereco")) {
            replacement = addressReplacements.computeIfAbsent(coreValue, this::createAddressReplacement);
        } else if (normalizedSubfield.equals("nome")) {
            replacement = companyNameReplacements.computeIfAbsent(coreValue, this::createCompanyNameReplacement);
        } else {
            replacement = coreValue;
        }

        return matcher.group(1) + replacement + matcher.group(3);
    }

    private String createAddressReplacement(String original) {
        String replacement;
        do {
            replacement = "Endereco Ficticio " + nextToken();
        } while (replacement.equals(original) || usedAddresses.contains(replacement));

        usedAddresses.add(replacement);
        return replacement;
    }

    private String createCompanyNameReplacement(String original) {
        String replacement;
        do {
            replacement = "Empresa Ficticia " + nextToken();
        } while (replacement.equals(original) || usedCompanyNames.contains(replacement));

        usedCompanyNames.add(replacement);
        return replacement;
    }

    private boolean isNoResponsiveRecord(String value) {
        return normalizedKey(value).equals("nenhum registro responsivo localizado");
    }

    private String createVanityNameReplacement(String original) {
        return createProfileReplacement(original);
    }

    private String createDescriptionReplacement(String ignored) {
        String replacement;
        do {
            replacement = "DESC" + nextToken() + randomLikeToken();
        } while (usedDescriptions.contains(replacement));

        usedDescriptions.add(replacement);
        return replacement;
    }

    private String createFirstNameReplacement(String original) {
        String replacement;
        do {
            replacement = nextFirstName < FIRST_NAMES.length
                    ? FIRST_NAMES[nextFirstName]
                    : "Nome" + nextToken();
            nextFirstName++;
        } while (replacement.equals(original) || usedFirstNames.contains(replacement));

        usedFirstNames.add(replacement);
        return replacement;
    }

    private String createLastNameReplacement(String original) {
        String replacement;
        do {
            replacement = nextLastName < LAST_NAMES.length
                    ? LAST_NAMES[nextLastName]
                    : "Sobrenome" + nextToken();
            nextLastName++;
        } while (replacement.equals(original) || usedLastNames.contains(replacement));

        usedLastNames.add(replacement);
        return replacement;
    }

    private String createMiddleNameReplacement(String original) {
        String replacement;
        do {
            replacement = nextMiddleName < FIRST_NAMES.length
                    ? FIRST_NAMES[nextMiddleName]
                    : "Meio" + nextToken();
            nextMiddleName++;
        } while (replacement.equals(original) || usedMiddleNames.contains(replacement));

        usedMiddleNames.add(replacement);
        return replacement;
    }

    private String fullNameReplacementFor(Element label, String original) {
        String composed = composeFullNameFromNearbyParts(label);
        if (composed != null && !composed.equals(original)) {
            return composed;
        }

        return fullNameReplacements.computeIfAbsent(original, this::createFullNameReplacement);
    }

    private String composeFullNameFromNearbyParts(Element label) {
        NameParts nameParts = nearbyNameParts(label);
        return composeFullName(nameParts);
    }

    private String composeFullName(NameParts nameParts) {
        if ((nameParts.firstName() == null || nameParts.firstName().isBlank())
                && (nameParts.lastName() == null || nameParts.lastName().isBlank())) {
            return null;
        }

        StringBuilder fullName = new StringBuilder();
        appendNamePart(fullName, nameParts.firstName());
        appendNamePart(fullName, nameParts.middleName());
        appendNamePart(fullName, nameParts.lastName());
        if (fullName.isEmpty()) {
            return null;
        }

        return fullName.toString();
    }

    private void appendNamePart(StringBuilder fullName, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (!fullName.isEmpty()) {
            fullName.append(' ');
        }

        fullName.append(value.trim());
    }

    private void recomposeFullNames(Document document) {
        for (Element fullNameLabel : document.select("div")) {
            if (fieldTypeFor(fullNameLabel) != FieldType.FULL_NAME) {
                continue;
            }

            Element valueElement = findValueElement(fullNameLabel);
            if (valueElement == null) {
                continue;
            }

            String composed = composeFullNameFromBlock(fullNameLabel);
            if (composed != null) {
                replaceFirstTextNode(valueElement, composed);
            }
        }
    }

    private String composeFullNameFromBlock(Element fullNameLabel) {
        Element block = nearestNameBlock(fullNameLabel);
        if (block == null) {
            return null;
        }

        return composeFullName(namePartsInBlock(block, fullNameLabel));
    }

    private Element nearestNameBlock(Element fullNameLabel) {
        Element current = fullNameLabel.parent();
        while (current != null && !"body".equalsIgnoreCase(current.tagName())) {
            NameParts parts = namePartsInBlock(current, fullNameLabel);
            if (parts.hasAnyValue()) {
                return current;
            }

            current = current.parent();
        }

        return null;
    }

    private NameParts namePartsInBlock(Element block, Element fullNameLabel) {
        String firstName = null;
        String middleName = null;
        String lastName = null;
        for (Element candidate : block.select("div")) {
            if (candidate == fullNameLabel) {
                continue;
            }

            FieldType candidateType = fieldTypeFor(candidate);
            if (candidateType != FieldType.FIRST_NAME
                    && candidateType != FieldType.MIDDLE_NAME
                    && candidateType != FieldType.LAST_NAME) {
                continue;
            }

            Element valueElement = findValueElement(candidate);
            if (valueElement == null) {
                continue;
            }

            String value = valueElement.text().trim();
            if (value.isEmpty() || containsPotentialFieldLabel(valueElement)) {
                continue;
            }

            if (candidateType == FieldType.FIRST_NAME) {
                firstName = value;
            } else if (candidateType == FieldType.MIDDLE_NAME) {
                middleName = value;
            } else {
                lastName = value;
            }
        }

        return new NameParts(firstName, middleName, lastName);
    }

    private NameParts nearbyNameParts(Element label) {
        Element parent = label.parent();
        if (parent == null) {
            return new NameParts(null, null, null);
        }

        String firstName = null;
        String middleName = null;
        String lastName = null;
        List<Element> labels = parent.select("div");
        for (Element candidate : labels) {
            if (candidate == label) {
                break;
            }

            FieldType candidateType = fieldTypeFor(candidate);
            if (candidateType != FieldType.FIRST_NAME
                    && candidateType != FieldType.MIDDLE_NAME
                    && candidateType != FieldType.LAST_NAME) {
                continue;
            }

            Element valueElement = findValueElement(candidate);
            if (valueElement == null) {
                continue;
            }

            String value = valueElement.text().trim();
            if (value.isEmpty() || containsPotentialFieldLabel(valueElement)) {
                continue;
            }

            if (candidateType == FieldType.FIRST_NAME) {
                firstName = value;
            } else if (candidateType == FieldType.MIDDLE_NAME) {
                middleName = value;
            } else {
                lastName = value;
            }
        }

        return new NameParts(firstName, middleName, lastName);
    }

    private String createFullNameReplacement(String original) {
        String replacement;
        do {
            replacement = createFirstNameReplacement(original) + " " + createLastNameReplacement(original);
        } while (replacement.equals(original) || usedFullNames.contains(replacement));

        usedFullNames.add(replacement);
        return replacement;
    }

    private String nextFootballTeam() {
        String team = FOOTBALL_TEAMS[nextTeam % FOOTBALL_TEAMS.length];
        nextTeam++;
        return team;
    }

    private String nextProfileToken() {
        return "profile_" + nextToken();
    }

    private String nextToken() {
        String token = Integer.toString(nextToken, 36).toLowerCase(Locale.ROOT);
        nextToken++;
        return token;
    }

    private String randomLikeToken() {
        String token = nextToken();
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            char digit = DESCRIPTION_DIGITS.charAt(i % DESCRIPTION_DIGITS.length());
            char letter = DESCRIPTION_LETTERS.charAt((token.charAt(i) + i) % DESCRIPTION_LETTERS.length());
            value.append(letter).append(digit);
        }
        return value.toString();
    }

    private String zeroPad(long value, int length) {
        String digits = Long.toString(value);
        if (digits.length() >= length) {
            return digits.substring(digits.length() - length);
        }

        return "0".repeat(length - digits.length()) + digits;
    }

    private enum FieldType {
        TARGET,
        INTERNAL_TICKET_NUMBER,
        DESCRIPTION,
        SUBJECT,
        PROFILE_IDENTIFIER,
        EMAIL,
        FIRST_NAME,
        LAST_NAME,
        MIDDLE_NAME,
        FULL_NAME,
        EMAIL_SUBFIELD_BLOCK,
        SMALL_BUSINESS_BLOCK;

        private boolean preservesNoResponsiveRecord() {
            return this == EMAIL
                    || this == EMAIL_SUBFIELD_BLOCK
                    || this == SMALL_BUSINESS_BLOCK;
        }
    }

    private record ProfileValueParts(String base, String identifier, String suffix) {
    }

    private record NameParts(String firstName, String middleName, String lastName) {
        private boolean hasAnyValue() {
            return (firstName != null && !firstName.isBlank())
                    || (middleName != null && !middleName.isBlank())
                    || (lastName != null && !lastName.isBlank());
        }
    }
}
