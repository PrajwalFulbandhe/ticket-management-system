package com.prajwal.tickets.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** A support ticket. Plain data holder (the rules live in TicketService). */
public class Ticket {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private int id;
    private String title;
    private String description;
    private Priority priority;
    private Status status;
    private String assignedTo;          // null until a ticket is assigned
    private final LocalDateTime createdAt;
    private LocalDateTime dueAt;
    private LocalDateTime closedAt;     // null until the ticket is closed

    /** Creates a brand-new ticket (status OPEN, no id yet). */
    public Ticket(String title, String description, Priority priority,
                  LocalDateTime createdAt, LocalDateTime dueAt) {
        this(0, title, description, priority, Status.OPEN, null, createdAt, dueAt, null);
    }

    /** Full constructor, used when a ticket is loaded from the database. */
    public Ticket(int id, String title, String description, Priority priority, Status status,
                  String assignedTo, LocalDateTime createdAt, LocalDateTime dueAt, LocalDateTime closedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.assignedTo = assignedTo;
        this.createdAt = createdAt;
        this.dueAt = dueAt;
        this.closedAt = closedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDueAt() { return dueAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    /** One-line summary used in the ticket list. */
    @Override
    public String toString() {
        String shortTitle = title.length() > 30 ? title.substring(0, 27) + "..." : title;
        return String.format("#%-3d %-8s %-11s %-30s | agent: %-8s | due: %s",
                id, priority, status, shortTitle,
                assignedTo == null ? "-" : assignedTo, dueAt.format(FMT));
    }

    /** Multi-line description used when viewing a single ticket. */
    public String toDetails() {
        return String.join(System.lineSeparator(),
                "Ticket #" + id,
                "  Title      : " + title,
                "  Description: " + (description.isEmpty() ? "-" : description),
                "  Priority   : " + priority + " (SLA " + priority.getSlaHours() + "h)",
                "  Status     : " + status,
                "  Assigned to: " + (assignedTo == null ? "-" : assignedTo),
                "  Created    : " + createdAt.format(FMT),
                "  Due        : " + dueAt.format(FMT),
                "  Closed     : " + (closedAt == null ? "-" : closedAt.format(FMT)));
    }
}
