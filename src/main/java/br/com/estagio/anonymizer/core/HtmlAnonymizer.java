package br.com.estagio.anonymizer.core;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class HtmlAnonymizer {
    private final PhoneAnonymizer phoneAnonymizer;
    private final TicketAnonymizer ticketAnonymizer;
    private final TableFieldAnonymizer tableFieldAnonymizer;
    private final SocialDivFieldAnonymizer socialDivFieldAnonymizer;

    public HtmlAnonymizer() {
        this(new PhoneAnonymizer(), new TicketAnonymizer());
    }

    HtmlAnonymizer(PhoneAnonymizer phoneAnonymizer, TicketAnonymizer ticketAnonymizer) {
        this.phoneAnonymizer = phoneAnonymizer;
        this.ticketAnonymizer = ticketAnonymizer;
        this.tableFieldAnonymizer = new TableFieldAnonymizer(ticketAnonymizer);
        this.socialDivFieldAnonymizer = new SocialDivFieldAnonymizer(ticketAnonymizer);
    }

    public String anonymize(String input) {
        return anonymize(input, AnonymizationListener.NONE);
    }

    public String anonymize(String input, AnonymizationListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");

        String withoutSocialDivFields = runStage(
                "SocialDivFieldAnonymizer",
                input,
                socialDivFieldAnonymizer::anonymize,
                listener
        );
        String withoutTableFields = runStage(
                "WhatsappTableFieldAnonymizer",
                withoutSocialDivFields,
                tableFieldAnonymizer::anonymize,
                listener
        );
        String withoutPhones = runStage("PhoneAnonymizer", withoutTableFields, phoneAnonymizer::anonymize, listener);
        return runStage("TicketAnonymizer", withoutPhones, ticketAnonymizer::anonymize, listener);
    }

    private String runStage(
            String stageName,
            String input,
            StageAnonymizer anonymizer,
            AnonymizationListener listener
    ) {
        listener.stageStarted(stageName);
        Instant start = Instant.now();
        String result = anonymizer.anonymize(input);
        listener.stageFinished(stageName, Duration.between(start, Instant.now()));
        return result;
    }

    private interface StageAnonymizer {
        String anonymize(String input);
    }
}
