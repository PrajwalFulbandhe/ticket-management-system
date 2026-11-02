package com.prajwal.tickets.model;

/**
 * Ticket priority. Each priority has an SLA: the number of hours within which
 * the ticket should be resolved. The due date of a ticket is calculated from it.
 */
public enum Priority {
    LOW(72),
    MEDIUM(24),
    HIGH(8),
    CRITICAL(4);

    private final int slaHours;

    Priority(int slaHours) {
        this.slaHours = slaHours;
    }

    public int getSlaHours() {
        return slaHours;
    }

    /** Parses user input such as "high" or " High " into a Priority. */
    public static Priority fromString(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Priority is required (LOW, MEDIUM, HIGH or CRITICAL).");
        }
        try {
            return Priority.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown priority '" + text.trim() + "'. Use LOW, MEDIUM, HIGH or CRITICAL.");
        }
    }
}
