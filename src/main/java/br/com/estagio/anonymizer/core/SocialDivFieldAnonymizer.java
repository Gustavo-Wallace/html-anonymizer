package br.com.estagio.anonymizer.core;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SocialDivFieldAnonymizer {
    private static final Pattern INSTAGRAM_OR_FACEBOOK_URL = Pattern.compile(
            "(?i)^(https?://(?:www\\.)?(?:instagram|facebook)\\.com/)([^/?#]+)(.*)$"
    );
    private static final Pattern EMAIL_WITH_SUFFIX = Pattern.compile("^([^\\s()]+@[^\\s()]+)(.*)$");
    private static final Pattern HTML_DOCUMENT_PATTERN = Pattern.compile("(?is)<\\s*html\\b");
    private static final String[] SUPPORTED_LABELS = {
            "target",
            "account identifier",
            "registered email addresses",
            "email",
            "vanity name",
            "first",
            "last",
            "full name"
    };

    private static final String[] FIRST_NAMES = {
            "Ana", "Bruno", "Carla", "Diego", "Maria", "Pedro", "Laura", "Rafael", "Julia", "Lucas"
    };
    private static final String[] LAST_NAMES = {
            "Silva", "Santos", "Oliveira", "Costa", "Souza", "Lima", "Pereira", "Almeida", "Rocha", "Mendes"
    };

    private final Map<String, String> targetReplacements = new HashMap<>();
    private final Map<String, String> accountIdentifierReplacements = new HashMap<>();
    private final Map<String, String> emailReplacements = new HashMap<>();
    private final Map<String, String> vanityNameReplacements = new HashMap<>();
    private final Map<String, String> firstNameReplacements = new HashMap<>();
    private final Map<String, String> lastNameReplacements = new HashMap<>();
    private final Map<String, String> fullNameReplacements = new HashMap<>();

    private final Set<String> usedTargets = new HashSet<>();
    private final Set<String> usedAccountIdentifiers = new HashSet<>();
    private final Set<String> usedEmails = new HashSet<>();
    private final Set<String> usedVanityNames = new HashSet<>();
    private final Set<String> usedFirstNames = new HashSet<>();
    private final Set<String> usedLastNames = new HashSet<>();
    private final Set<String> usedFullNames = new HashSet<>();

    private long nextTarget = 1;
    private int nextToken = 1;
    private int nextFirstName;
    private int nextLastName;

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

        String replacement = replacementFor(fieldType, original);
        replaceFirstTextNode(valueElement, replacement);
    }

    private FieldType fieldTypeFor(Element label) {
        String labelText = normalizedText(label);

        if (isLabel(labelText, "Target")) {
            return FieldType.TARGET;
        }

        if (isLabel(labelText, "Account Identifier")) {
            return FieldType.ACCOUNT_IDENTIFIER;
        }

        if (isLabel(labelText, "Registered Email Addresses") || isLabel(labelText, "Email")) {
            return FieldType.EMAIL;
        }

        if (isLabel(labelText, "Vanity Name")) {
            return FieldType.VANITY_NAME;
        }

        if (isLabel(labelText, "First")) {
            return FieldType.FIRST_NAME;
        }

        if (isLabel(labelText, "Last")) {
            return FieldType.LAST_NAME;
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

        Element siblingValue = firstNextElementSiblingWithClass(label, "m");
        if (siblingValue != null) {
            return siblingValue;
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
            case ACCOUNT_IDENTIFIER -> accountIdentifierReplacements.computeIfAbsent(
                    original,
                    this::createAccountIdentifierReplacement
            );
            case EMAIL -> emailReplacements.computeIfAbsent(original, this::createEmailReplacement);
            case VANITY_NAME -> vanityNameReplacements.computeIfAbsent(original, this::createVanityNameReplacement);
            case FIRST_NAME -> firstNameReplacements.computeIfAbsent(original, this::createFirstNameReplacement);
            case LAST_NAME -> lastNameReplacements.computeIfAbsent(original, this::createLastNameReplacement);
            case FULL_NAME -> fullNameReplacements.computeIfAbsent(original, this::createFullNameReplacement);
        };
    }

    private String normalizedText(Element element) {
        String ownText = element.ownText().replaceAll("\\s+", " ").trim();
        if (!ownText.isEmpty() || !element.hasClass("t") || !element.hasClass("i")) {
            return ownText;
        }

        return element.text().replaceAll("\\s+", " ").trim();
    }

    private boolean isLabel(String value, String expected) {
        return value.equalsIgnoreCase(expected);
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

    private String createAccountIdentifierReplacement(String original) {
        String replacement;
        do {
            Matcher matcher = INSTAGRAM_OR_FACEBOOK_URL.matcher(original);
            String token = nextProfileToken();
            replacement = matcher.matches() ? matcher.group(1) + token + matcher.group(3) : token;
        } while (replacement.equals(original) || usedAccountIdentifiers.contains(replacement));

        usedAccountIdentifiers.add(replacement);
        return replacement;
    }

    private String createEmailReplacement(String original) {
        String replacement;
        do {
            Matcher matcher = EMAIL_WITH_SUFFIX.matcher(original);
            String suffix = matcher.matches() ? matcher.group(2) : "";
            replacement = "user_" + nextToken() + "@example.com" + suffix;
        } while (replacement.equals(original) || usedEmails.contains(replacement));

        usedEmails.add(replacement);
        return replacement;
    }

    private String createVanityNameReplacement(String original) {
        String replacement;
        do {
            replacement = nextProfileToken();
        } while (replacement.equals(original) || usedVanityNames.contains(replacement));

        usedVanityNames.add(replacement);
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

    private String createFullNameReplacement(String original) {
        String replacement;
        do {
            replacement = createFirstNameReplacement(original) + " " + createLastNameReplacement(original);
        } while (replacement.equals(original) || usedFullNames.contains(replacement));

        usedFullNames.add(replacement);
        return replacement;
    }

    private String nextProfileToken() {
        return "profile_" + nextToken();
    }

    private String nextToken() {
        String token = Integer.toString(nextToken, 36).toLowerCase(Locale.ROOT);
        nextToken++;
        return token;
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
        ACCOUNT_IDENTIFIER,
        EMAIL,
        VANITY_NAME,
        FIRST_NAME,
        LAST_NAME,
        FULL_NAME
    }
}
