package br.com.estagio.anonymizer.core;

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
        String withoutSocialDivFields = socialDivFieldAnonymizer.anonymize(input);
        String withoutTableFields = tableFieldAnonymizer.anonymize(withoutSocialDivFields);
        String withoutPhones = phoneAnonymizer.anonymize(withoutTableFields);
        return ticketAnonymizer.anonymize(withoutPhones);
    }
}
