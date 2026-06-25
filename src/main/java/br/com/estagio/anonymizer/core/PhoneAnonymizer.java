package br.com.estagio.anonymizer.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneAnonymizer {
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<![\\d+])\\+?\\d(?:[ -]?\\d){7,14}(?!\\d)");

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
            String normalized = normalize(phone);
            String replacementDigits = replacements.computeIfAbsent(normalized, this::createReplacement);
            matcher.appendReplacement(result, Matcher.quoteReplacement(applyFormatting(phone, replacementDigits)));
        }

        matcher.appendTail(result);
        return result.toString();
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
