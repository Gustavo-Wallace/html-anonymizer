package br.com.estagio.anonymizer.core;

public class HtmlAnonymizer {
    private final PhoneAnonymizer phoneAnonymizer;
    private final TicketAnonymizer ticketAnonymizer;

    public HtmlAnonymizer() {
        this(new PhoneAnonymizer(), new TicketAnonymizer());
    }

    HtmlAnonymizer(PhoneAnonymizer phoneAnonymizer, TicketAnonymizer ticketAnonymizer) {
        this.phoneAnonymizer = phoneAnonymizer;
        this.ticketAnonymizer = ticketAnonymizer;
    }

    public String anonymize(String input) {
        String withoutPhones = phoneAnonymizer.anonymize(input);
        return ticketAnonymizer.anonymize(withoutPhones);
    }
}
