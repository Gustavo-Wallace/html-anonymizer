package br.com.estagio.anonymizer.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneAnonymizer {
    private static final int CONTEXT_LOOKBACK = 1200;
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<![\\d+])\\+?\\d(?:[ -]?\\d){7,14}(?!\\d)");
    private static final Pattern TICKET_PREFIX_PATTERN = Pattern.compile("Internal Ticket Number[\\t ]*:?[\\t ]*$");
    private static final Pattern TICKET_TABLE_PREFIX_PATTERN = Pattern.compile(
            "(?is)<tr\\b[^>]*>(?:(?!</tr>).)*<th\\b[^>]*>\\s*Internal Ticket Number\\s*</th\\s*>"
                    + "(?:(?!</tr>).)*<td\\b[^>]*>[^<]*$"
    );
    private static final Pattern TARGET_DIV_PREFIX_PATTERN = Pattern.compile(
            "(?is)<div\\b[^>]*>\\s*Target\\s*</div\\s*>\\s*<div\\b[^>]*>[^<]*$"
    );
    private static final Pattern SOCIAL_LABEL_PATTERN = Pattern.compile(
            "(?is)<div\\b[^>]*>\\s*(Target|Account Identifier|Registered Email Addresses|Email|Vanity Name|First|Last|Full Name)\\s*</div\\s*>"
    );

    private final Map<String, String> replacements = new HashMap<>();
    private final Set<String> usedReplacements = new HashSet<>();
    private final Random random;

    public PhoneAnonymizer() {
        this(new Random());
    }

    PhoneAnonymizer(Random random) {
        this.random = Objects.requireNonNull(random);
    }

    public String anonymize(String input) {
        if (input == null) {
            return null;
        }

        Matcher matcher = PHONE_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String phone = matcher.group();
            if (isInternalTicketNumber(input, matcher.start())) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(phone));
                continue;
            }

            String normalized = normalize(phone);
            String replacementDigits = replacements.computeIfAbsent(normalized, this::createReplacement);
            matcher.appendReplacement(result, Matcher.quoteReplacement(applyFormatting(phone, replacementDigits)));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private boolean isInternalTicketNumber(String input, int phoneStart) {
        String prefix = input.substring(Math.max(0, phoneStart - CONTEXT_LOOKBACK), phoneStart);
        return TICKET_PREFIX_PATTERN.matcher(prefix).find()
                || TICKET_TABLE_PREFIX_PATTERN.matcher(prefix).find()
                || TARGET_DIV_PREFIX_PATTERN.matcher(prefix).find()
                || isSocialTargetValue(prefix);
    }

    private boolean isSocialTargetValue(String prefix) {
        if (!isInsideOpenDiv(prefix)) {
            return false;
        }

        Matcher matcher = SOCIAL_LABEL_PATTERN.matcher(prefix);
        String lastLabel = null;
        while (matcher.find()) {
            lastLabel = matcher.group(1);
        }

        return "Target".equalsIgnoreCase(lastLabel);
    }

    private boolean isInsideOpenDiv(String prefix) {
        String lowerCasePrefix = prefix.toLowerCase(Locale.ROOT);
        int lastOpenDiv = lowerCasePrefix.lastIndexOf("<div");
        int lastCloseDiv = lowerCasePrefix.lastIndexOf("</div");
        return lastOpenDiv > lastCloseDiv;
    }

    private String createReplacement(String originalDigits) {
        String replacement;

        do {
            replacement = randomDigits(originalDigits.length());
        } while (replacement.equals(originalDigits) || usedReplacements.contains(replacement));

        usedReplacements.add(replacement);
        return replacement;
    }

    private String randomDigits(int length) {
        StringBuilder digits = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            digits.append(random.nextInt(10));
        }

        return digits.toString();
    }

    private String applyFormatting(String original, String replacementDigits) {
        StringBuilder formatted = new StringBuilder(original.length());
        int digitIndex = 0;

        for (int i = 0; i < original.length(); i++) {
            char current = original.charAt(i);
            if (Character.isDigit(current)) {
                formatted.append(replacementDigits.charAt(digitIndex));
                digitIndex++;
            } else {
                formatted.append(current);
            }
        }

        return formatted.toString();
    }

    private String normalize(String phone) {
        StringBuilder digits = new StringBuilder(phone.length());

        for (int i = 0; i < phone.length(); i++) {
            char current = phone.charAt(i);
            if (Character.isDigit(current)) {
                digits.append(current);
            }
        }

        return digits.toString();
    }
}
