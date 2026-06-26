package br.com.estagio.anonymizer.core;

public class HtmlAnonymizer {
    private final PhoneAnonymizer phoneAnonymizer;
    private final TicketAnonymizer ticketAnonymizer;
    private final TableFieldAnonymizer tableFieldAnonymizer;

    public HtmlAnonymizer() {
        this(new PhoneAnonymizer(), new TicketAnonymizer());
    }

    HtmlAnonymizer(PhoneAnonymizer phoneAnonymizer, TicketAnonymizer ticketAnonymizer) {
        this.phoneAnonymizer = phoneAnonymizer;
        this.ticketAnonymizer = ticketAnonymizer;
        this.tableFieldAnonymizer = new TableFieldAnonymizer(ticketAnonymizer);
    }

    public String anonymize(String input) {
        String withoutTableFields = tableFieldAnonymizer.anonymize(input);
        String withoutPhones = phoneAnonymizer.anonymize(withoutTableFields);
        return ticketAnonymizer.anonymize(withoutPhones);
    }
}
