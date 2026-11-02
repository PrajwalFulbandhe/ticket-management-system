package com.prajwal.tickets.service;

import com.prajwal.tickets.model.Priority;
import com.prajwal.tickets.model.Status;
import com.prajwal.tickets.model.Ticket;
import com.prajwal.tickets.repository.TicketRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Supplier;

/**
 * Business rules for tickets: validation, SLA due dates and allowed status changes.
 * It talks to storage only through the TicketRepository interface, so the same
 * rules work with MySQL or with the in-memory repository used in tests.
 */
public class TicketService {

    private static final int MAX_TITLE = 100;
    private static final int MAX_AGENT = 50;

    private final TicketRepository repository;
    private final Supplier<LocalDateTime> clock; // injectable so tests can control "now"

    public TicketService(TicketRepository repository) {
        this(repository, LocalDateTime::now);
    }

    public TicketService(TicketRepository repository, Supplier<LocalDateTime> clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Creates an OPEN ticket whose due date is "now + SLA hours of the priority". */
    public Ticket createTicket(String title, String description, Priority priority) {
        String cleanTitle = requireText(title, "Title", MAX_TITLE);
        if (priority == null) {
            throw new IllegalArgumentException("Priority is required.");
        }
        String cleanDescription = description == null ? "" : description.trim();
        LocalDateTime now = clock.get();
        Ticket ticket = new Ticket(cleanTitle, cleanDescription, priority,
                now, now.plusHours(priority.getSlaHours()));
        return repository.save(ticket);
    }

    public Ticket getTicket(int id) {
        return repository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    public List<Ticket> listAll() {
        return repository.findAll();
    }

    public List<Ticket> listByStatus(Status status) {
        return repository.findByStatus(status);
    }

    /** Assigns a ticket to an agent. An OPEN ticket automatically becomes IN_PROGRESS. */
    public Ticket assign(int id, String agent) {
        String cleanAgent = requireText(agent, "Agent name", MAX_AGENT);
        Ticket ticket = getTicket(id);
        if (ticket.getStatus() == Status.CLOSED) {
            throw new IllegalStateException("Ticket #" + id + " is closed and cannot be assigned.");
        }
        ticket.setAssignedTo(cleanAgent);
        if (ticket.getStatus() == Status.OPEN) {
            ticket.setStatus(Status.IN_PROGRESS);
        }
        repository.update(ticket);
        return ticket;
    }

    /** Moves a ticket to a new status if the lifecycle allows it. */
    public Ticket changeStatus(int id, Status newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status is required.");
        }
        Ticket ticket = getTicket(id);
        if (!ticket.getStatus().canMoveTo(newStatus)) {
            throw new IllegalStateException(String.format(
                    "Ticket #%d cannot move from %s to %s.", id, ticket.getStatus(), newStatus));
        }
        ticket.setStatus(newStatus);
        if (newStatus == Status.CLOSED) {
            ticket.setClosedAt(clock.get());
        }
        repository.update(ticket);
        return ticket;
    }

    public void deleteTicket(int id) {
        if (!repository.delete(id)) {
            throw new TicketNotFoundException(id);
        }
    }

    /** OPEN / IN_PROGRESS tickets that have passed their SLA due date. */
    public List<Ticket> overdueTickets() {
        return repository.findOverdue(clock.get());
    }

    public Map<Status, Integer> statusReport() {
        return repository.countByStatus();
    }

    public Map<String, Integer> agentWorkload() {
        return repository.countByAgent();
    }

    public OptionalDouble averageResolutionMinutes() {
        return repository.averageResolutionMinutes();
    }

    // ---- helpers ----------------------------------------------------------

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " cannot be longer than " + maxLength + " characters.");
        }
        return trimmed;
    }
}
