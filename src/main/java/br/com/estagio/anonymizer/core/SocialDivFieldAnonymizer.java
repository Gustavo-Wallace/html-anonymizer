package br.com.estagio.anonymizer.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SocialDivFieldAnonymizer {
    private static final Pattern DIV_PATTERN = Pattern.compile("(?is)(<div\\b[^>]*>)(.*?)(</div\\s*>)");
    private static final Pattern TAG_PATTERN = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern INSTAGRAM_OR_FACEBOOK_URL = Pattern.compile(
            "(?i)^(https?://(?:www\\.)?(?:instagram|facebook)\\.com/)([^/?#]+)(.*)$"
    );
    private static final Pattern EMAIL_WITH_SUFFIX = Pattern.compile("^([^\\s()]+@[^\\s()]+)(.*)$");

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

        List<DivElement> divs = findDivs(input);
        if (divs.isEmpty()) {
            return input;
        }

        Map<Integer, String> replacementsByDivIndex = new HashMap<>();
        for (int i = 0; i < divs.size(); i++) {
            String label = divs.get(i).visibleText();

            if (isLabel(label, "Target")) {
                replaceNextValue(divs, replacementsByDivIndex, i, targetReplacements, this::createTargetReplacement);
            } else if (isLabel(label, "Account Identifier")) {
                replaceNextValue(
                        divs,
                        replacementsByDivIndex,
                        i,
                        accountIdentifierReplacements,
                        this::createAccountIdentifierReplacement
                );
            } else if (isLabel(label, "Registered Email Addresses")) {
                replaceNextValue(divs, replacementsByDivIndex, i, emailReplacements, this::createEmailReplacement);
            } else if (isLabel(label, "Email")) {
                replaceNextValue(divs, replacementsByDivIndex, i, emailReplacements, this::createEmailReplacement);
            } else if (isLabel(label, "Vanity Name")) {
                replaceNextValue(divs, replacementsByDivIndex, i, vanityNameReplacements, this::createVanityNameReplacement);
            } else if (isLabel(label, "Name") && hasNextLabel(divs, i, "First")) {
                replaceValueAt(divs, replacementsByDivIndex, i + 2, firstNameReplacements, this::createFirstNameReplacement);
            } else if (isLabel(label, "Last")) {
                replaceNextValue(divs, replacementsByDivIndex, i, lastNameReplacements, this::createLastNameReplacement);
            } else if (isLabel(label, "Middle Name") && hasNextLabel(divs, i, "Full Name")) {
                replaceValueAt(divs, replacementsByDivIndex, i + 2, fullNameReplacements, this::createFullNameReplacement);
            }
        }

        return rebuild(input, divs, replacementsByDivIndex);
    }

    private List<DivElement> findDivs(String input) {
        Matcher matcher = DIV_PATTERN.matcher(input);
        List<DivElement> divs = new ArrayList<>();

        while (matcher.find()) {
            divs.add(new DivElement(
                    matcher.start(),
                    matcher.end(),
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3)
            ));
        }

        return divs;
    }

    private void replaceNextValue(
            List<DivElement> divs,
            Map<Integer, String> replacementsByDivIndex,
            int labelIndex,
            Map<String, String> replacements,
            ReplacementFactory replacementFactory
    ) {
        replaceValueAt(divs, replacementsByDivIndex, labelIndex + 1, replacements, replacementFactory);
    }

    private void replaceValueAt(
            List<DivElement> divs,
            Map<Integer, String> replacementsByDivIndex,
            int valueIndex,
            Map<String, String> replacements,
            ReplacementFactory replacementFactory
    ) {
        if (valueIndex >= divs.size()) {
            return;
        }

        DivElement value = divs.get(valueIndex);
        String original = value.visibleText();
        if (original.isEmpty()) {
            return;
        }

        String replacement = replacements.computeIfAbsent(original, replacementFactory::create);
        replacementsByDivIndex.put(valueIndex, value.withContent(replacement));
    }

    private String rebuild(String input, List<DivElement> divs, Map<Integer, String> replacementsByDivIndex) {
        StringBuilder result = new StringBuilder(input.length());
        int last = 0;

        for (int i = 0; i < divs.size(); i++) {
            DivElement div = divs.get(i);
            result.append(input, last, div.start());
            result.append(replacementsByDivIndex.getOrDefault(i, div.original()));
            last = div.end();
        }

        result.append(input.substring(last));
        return result.toString();
    }

    private boolean hasNextLabel(List<DivElement> divs, int index, String expectedLabel) {
        return index + 1 < divs.size() && isLabel(divs.get(index + 1).visibleText(), expectedLabel);
    }

    private boolean isLabel(String value, String expected) {
        return value.equalsIgnoreCase(expected);
    }

    private String visibleText(String html) {
        String withoutTags = TAG_PATTERN.matcher(html).replaceAll(" ");
        return withoutTags.replaceAll("\\s+", " ").trim();
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
        String token = Integer.toString(nextToken, 36);
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

    private class DivElement {
        private final int start;
        private final int end;
        private final String openingTag;
        private final String content;
        private final String closingTag;

        DivElement(int start, int end, String openingTag, String content, String closingTag) {
            this.start = start;
            this.end = end;
            this.openingTag = openingTag;
            this.content = content;
            this.closingTag = closingTag;
        }

        int start() {
            return start;
        }

        int end() {
            return end;
        }

        String visibleText() {
            return SocialDivFieldAnonymizer.this.visibleText(content);
        }

        String original() {
            return openingTag + content + closingTag;
        }

        String withContent(String replacement) {
            return openingTag + replacement + closingTag;
        }
    }

    private interface ReplacementFactory {
        String create(String original);
    }
}
