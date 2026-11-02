package com.prajwal.tickets.model;

/**
 * Ticket lifecycle:
 * OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
 * A ticket can go back from IN_PROGRESS to OPEN, and from RESOLVED back to
 * IN_PROGRESS (re-opened). A CLOSED ticket can never change again.
 */
public enum Status {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    /** Returns true if moving from this status to {@code next} is allowed. */
    public boolean canMoveTo(Status next) {
        return switch (this) {
            case OPEN -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == OPEN || next == RESOLVED;
            case RESOLVED -> next == IN_PROGRESS || next == CLOSED;
            case CLOSED -> false;
        };
    }

    /** Parses user input such as "in_progress" into a Status. */
    public static Status fromString(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Status is required (OPEN, IN_PROGRESS, RESOLVED or CLOSED).");
        }
        try {
            return Status.valueOf(text.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown status '" + text.trim() + "'. Use OPEN, IN_PROGRESS, RESOLVED or CLOSED.");
        }
    }
}
