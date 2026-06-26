package br.com.estagio.anonymizer.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TicketAnonymizer {
    private static final Pattern TICKET_PATTERN = Pattern.compile("(Internal Ticket Number)([\\t ]*:?[\\t ]*)(\\d+)");

    private final Map<String, String> replacements = new HashMap<>();
    private final Set<String> usedReplacements = new HashSet<>();
    private long nextValue = 1;

    public String anonymize(String input) {
        if (input == null) {
            return null;
        }

        Matcher matcher = TICKET_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String ticket = matcher.group(3);
            String replacement = anonymizeTicketValue(ticket);
            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(matcher.group(1) + matcher.group(2) + replacement)
            );
        }

        matcher.appendTail(result);
        return result.toString();
    }

    String anonymizeTicketValue(String ticket) {
        return replacements.computeIfAbsent(ticket, this::createReplacement);
    }

    private String createReplacement(String originalTicket) {
        String replacement;

        do {
            replacement = zeroPad(nextValue, originalTicket.length());
            nextValue++;
        } while (replacement.equals(originalTicket) || usedReplacements.contains(replacement));

        usedReplacements.add(replacement);
        return replacement;
    }

    private String zeroPad(long value, int length) {
        String digits = Long.toString(value);
        if (digits.length() >= length) {
            return digits.substring(digits.length() - length);
        }

        return "0".repeat(length - digits.length()) + digits;
    }
}
