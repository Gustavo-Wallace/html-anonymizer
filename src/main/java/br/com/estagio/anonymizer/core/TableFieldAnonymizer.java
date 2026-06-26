package br.com.estagio.anonymizer.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TableFieldAnonymizer {
    private static final Pattern ROW_PATTERN = Pattern.compile("(?is)(<tr\\b[^>]*>)(.*?)(</tr\\s*>)");
    private static final Pattern TD_PATTERN = Pattern.compile("(?is)(<td\\b[^>]*>)(.*?)(</td\\s*>)");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final Pattern TAG_PATTERN = Pattern.compile("(?is)<[^>]+>");
    private static final String DESCRIPTION_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String DESCRIPTION_DIGITS = "0123456789";
    private static final String[] FOOTBALL_TEAMS = {
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
    };

    private final TicketAnonymizer ticketAnonymizer;
    private final Map<String, String> descriptionReplacements = new HashMap<>();
    private final Map<String, String> subjectReplacements = new HashMap<>();
    private final Set<String> usedDescriptionReplacements = new HashSet<>();
    private final Random random;
    private int nextTeamIndex;

    TableFieldAnonymizer(TicketAnonymizer ticketAnonymizer) {
        this(ticketAnonymizer, new Random());
    }

    TableFieldAnonymizer(TicketAnonymizer ticketAnonymizer, Random random) {
        this.ticketAnonymizer = Objects.requireNonNull(ticketAnonymizer);
        this.random = Objects.requireNonNull(random);
    }

    String anonymize(String input) {
        if (input == null) {
            return null;
        }

        Matcher matcher = ROW_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String row = matcher.group(1) + anonymizeRowContent(matcher.group(2)) + matcher.group(3);
            matcher.appendReplacement(result, Matcher.quoteReplacement(row));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String anonymizeRowContent(String rowContent) {
        if (hasHeader(rowContent, "Internal Ticket Number")) {
            return replaceTdContent(rowContent, this::anonymizeTicketTd);
        }

        if (hasHeader(rowContent, "Description")) {
            return replaceTdContent(rowContent, this::anonymizeDescriptionTd);
        }

        if (hasHeader(rowContent, "Subject")) {
            return replaceTdContent(rowContent, this::anonymizeSubjectTd);
        }

        return rowContent;
    }

    private boolean hasHeader(String rowContent, String header) {
        String expression = "(?is)<th\\b[^>]*>\\s*" + Pattern.quote(header) + "\\s*</th\\s*>";
        return Pattern.compile(expression).matcher(rowContent).find();
    }

    private String replaceTdContent(String rowContent, TdContentAnonymizer anonymizer) {
        Matcher matcher = TD_PATTERN.matcher(rowContent);
        if (!matcher.find()) {
            return rowContent;
        }

        String replacement = matcher.group(1) + anonymizer.anonymize(matcher.group(2)) + matcher.group(3);
        return rowContent.substring(0, matcher.start()) + replacement + rowContent.substring(matcher.end());
    }

    private String anonymizeTicketTd(String tdContent) {
        Matcher matcher = NUMBER_PATTERN.matcher(tdContent);
        if (!matcher.find()) {
            return tdContent;
        }

        String replacement = ticketAnonymizer.anonymizeTicketValue(matcher.group());
        return tdContent.substring(0, matcher.start()) + replacement + tdContent.substring(matcher.end());
    }

    private String anonymizeDescriptionTd(String tdContent) {
        String originalText = visibleText(tdContent);
        if (originalText.isEmpty()) {
            return tdContent;
        }

        String replacement = descriptionReplacements.computeIfAbsent(originalText, this::createDescriptionReplacement);
        return replaceTextKeepingTags(tdContent, replacement);
    }

    private String anonymizeSubjectTd(String tdContent) {
        String originalText = visibleText(tdContent);
        if (originalText.isEmpty()) {
            return tdContent;
        }

        String replacement = subjectReplacements.computeIfAbsent(originalText, ignored -> nextFootballTeam());
        return replaceTextKeepingTags(tdContent, replacement);
    }

    private String visibleText(String html) {
        String withoutTags = TAG_PATTERN.matcher(html).replaceAll(" ");
        return withoutTags.replaceAll("\\s+", " ").trim();
    }

    private String replaceTextKeepingTags(String html, String replacement) {
        Matcher matcher = TAG_PATTERN.matcher(html);
        StringBuilder result = new StringBuilder();
        int last = 0;
        boolean replacementUsed = false;

        while (matcher.find()) {
            replacementUsed = appendTextSegment(result, html.substring(last, matcher.start()), replacement, replacementUsed);
            result.append(matcher.group());
            last = matcher.end();
        }

        appendTextSegment(result, html.substring(last), replacement, replacementUsed);
        return result.toString();
    }

    private boolean appendTextSegment(
            StringBuilder result,
            String segment,
            String replacement,
            boolean replacementUsed
    ) {
        if (segment.trim().isEmpty()) {
            result.append(segment);
            return replacementUsed;
        }

        int start = 0;
        while (start < segment.length() && Character.isWhitespace(segment.charAt(start))) {
            start++;
        }

        int end = segment.length();
        while (end > start && Character.isWhitespace(segment.charAt(end - 1))) {
            end--;
        }

        result.append(segment, 0, start);
        if (!replacementUsed) {
            result.append(replacement);
        }
        result.append(segment.substring(end));
        return true;
    }

    private String createDescriptionReplacement(String ignored) {
        String replacement;

        do {
            replacement = randomDescriptionValue();
        } while (usedDescriptionReplacements.contains(replacement));

        usedDescriptionReplacements.add(replacement);
        return replacement;
    }

    private String randomDescriptionValue() {
        StringBuilder value = new StringBuilder("DESC");

        for (int i = 0; i < 6; i++) {
            value.append(DESCRIPTION_LETTERS.charAt(random.nextInt(DESCRIPTION_LETTERS.length())));
            value.append(DESCRIPTION_DIGITS.charAt(random.nextInt(DESCRIPTION_DIGITS.length())));
        }

        return value.toString();
    }

    private String nextFootballTeam() {
        String team = FOOTBALL_TEAMS[nextTeamIndex % FOOTBALL_TEAMS.length];
        nextTeamIndex++;
        return team;
    }

    private interface TdContentAnonymizer {
        String anonymize(String tdContent);
    }
}
