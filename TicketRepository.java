package com.prajwal.tickets.repository;

import com.prajwal.tickets.model.Status;
import com.prajwal.tickets.model.Ticket;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Storage contract for tickets. There are two implementations:
 * JdbcTicketRepository (MySQL/PostgreSQL) and InMemoryTicketRepository
 * (demo mode and unit tests). TicketService only knows this interface.
 */
public interface TicketRepository {

    /** Stores a new ticket, sets its generated id and returns it. */
    Ticket save(Ticket ticket);

    Optional<Ticket> findById(int id);

    List<Ticket> findAll();

    List<Ticket> findByStatus(Status status);

    /** Writes the changed fields of an existing ticket. */
    void update(Ticket ticket);

    /** Returns true if a ticket was deleted. */
    boolean delete(int id);

    /** OPEN or IN_PROGRESS tickets whose due date is before {@code now}. */
    List<Ticket> findOverdue(LocalDateTime now);

    /** Number of tickets per status (every status is present, even with 0). */
    Map<Status, Integer> countByStatus();

    /** Number of assigned tickets per agent, busiest agent first. */
    Map<String, Integer> countByAgent();

    /** Average minutes from creation to closing, over CLOSED tickets. */
    OptionalDouble averageResolutionMinutes();
}
