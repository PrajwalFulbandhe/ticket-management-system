package com.prajwal.tickets.repository;

import com.prajwal.tickets.model.Status;
import com.prajwal.tickets.model.Ticket;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;

/** Keeps tickets in memory. Used for unit tests and for the no-database demo mode. */
public class InMemoryTicketRepository implements TicketRepository {

    private final Map<Integer, Ticket> tickets = new TreeMap<>();
    private int nextId = 1;

    @Override
    public Ticket save(Ticket ticket) {
        ticket.setId(nextId++);
        tickets.put(ticket.getId(), ticket);
        return ticket;
    }

    @Override
    public Optional<Ticket> findById(int id) {
        return Optional.ofNullable(tickets.get(id));
    }

    @Override
    public List<Ticket> findAll() {
        return new ArrayList<>(tickets.values());
    }

    @Override
    public List<Ticket> findByStatus(Status status) {
        return tickets.values().stream().filter(t -> t.getStatus() == status).toList();
    }

    @Override
    public void update(Ticket ticket) {
        tickets.put(ticket.getId(), ticket);
    }

    @Override
    public boolean delete(int id) {
        return tickets.remove(id) != null;
    }

    @Override
    public List<Ticket> findOverdue(LocalDateTime now) {
        return tickets.values().stream()
                .filter(t -> t.getStatus() == Status.OPEN || t.getStatus() == Status.IN_PROGRESS)
                .filter(t -> t.getDueAt().isBefore(now))
                .sorted((a, b) -> a.getDueAt().compareTo(b.getDueAt()))
                .toList();
    }

    @Override
    public Map<Status, Integer> countByStatus() {
        Map<Status, Integer> counts = new EnumMap<>(Status.class);
        for (Status s : Status.values()) {
            counts.put(s, 0);
        }
        tickets.values().forEach(t -> counts.merge(t.getStatus(), 1, Integer::sum));
        return counts;
    }

    @Override
    public Map<String, Integer> countByAgent() {
        Map<String, Integer> counts = new TreeMap<>();
        tickets.values().stream()
                .filter(t -> t.getAssignedTo() != null)
                .forEach(t -> counts.merge(t.getAssignedTo(), 1, Integer::sum));
        // busiest agent first, ties broken alphabetically
        Map<String, Integer> sorted = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    @Override
    public OptionalDouble averageResolutionMinutes() {
        return tickets.values().stream()
                .filter(t -> t.getStatus() == Status.CLOSED && t.getClosedAt() != null)
                .mapToLong(t -> Duration.between(t.getCreatedAt(), t.getClosedAt()).toMinutes())
                .average();
    }
}
