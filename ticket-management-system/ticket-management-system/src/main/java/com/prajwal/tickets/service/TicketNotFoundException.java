package com.prajwal.tickets.service;

/** Thrown when a ticket id does not exist. */
public class TicketNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TicketNotFoundException(int id) {
        super("Ticket #" + id + " was not found.");
    }
}
