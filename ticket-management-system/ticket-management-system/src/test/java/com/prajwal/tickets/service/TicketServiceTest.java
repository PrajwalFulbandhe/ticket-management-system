package com.prajwal.tickets.service;

import com.prajwal.tickets.model.Priority;
import com.prajwal.tickets.model.Status;
import com.prajwal.tickets.model.Ticket;
import com.prajwal.tickets.repository.InMemoryTicketRepository;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TicketServiceTest {

    // A clock we control, so SLA and overdue logic can be tested without waiting.
    private final AtomicReference<LocalDateTime> now =
            new AtomicReference<>(LocalDateTime.of(2026, 11, 2, 9, 0));
    private TicketService service;

    @Before
    public void setUp() {
        service = new TicketService(new InMemoryTicketRepository(), now::get);
    }

    @Test
    public void newTicketIsOpenWithSlaDueDate() {
        Ticket t = service.createTicket("  VPN down  ", "Cannot connect", Priority.HIGH);

        assertEquals(1, t.getId());
        assertEquals("VPN down", t.getTitle());              // trimmed
        assertEquals(Status.OPEN, t.getStatus());
        assertEquals(now.get().plusHours(8), t.getDueAt());   // HIGH = 8 hours
        assertNull(t.getAssignedTo());
    }

    @Test
    public void slaDependsOnPriority() {
        assertEquals(now.get().plusHours(4), service.createTicket("a", "", Priority.CRITICAL).getDueAt());
        assertEquals(now.get().plusHours(24), service.createTicket("b", "", Priority.MEDIUM).getDueAt());
        assertEquals(now.get().plusHours(72), service.createTicket("c", "", Priority.LOW).getDueAt());
    }

    @Test
    public void blankTitleIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.createTicket("   ", "x", Priority.LOW));
        assertThrows(IllegalArgumentException.class, () -> service.createTicket(null, "x", Priority.LOW));
    }

    @Test
    public void tooLongTitleIsRejected() {
        String longTitle = "x".repeat(101);
        assertThrows(IllegalArgumentException.class, () -> service.createTicket(longTitle, "x", Priority.LOW));
    }

    @Test
    public void missingPriorityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.createTicket("Title", "x", null));
    }

    @Test
    public void assigningOpenTicketMovesItToInProgress() {
        Ticket t = service.createTicket("Printer jam", "", Priority.LOW);
        service.assign(t.getId(), "Anita");

        Ticket saved = service.getTicket(t.getId());
        assertEquals("Anita", saved.getAssignedTo());
        assertEquals(Status.IN_PROGRESS, saved.getStatus());
    }

    @Test
    public void blankAgentIsRejected() {
        Ticket t = service.createTicket("Printer jam", "", Priority.LOW);
        assertThrows(IllegalArgumentException.class, () -> service.assign(t.getId(), " "));
    }

    @Test
    public void openTicketCannotBeClosedDirectly() {
        Ticket t = service.createTicket("Laptop slow", "", Priority.MEDIUM);
        assertThrows(IllegalStateException.class, () -> service.changeStatus(t.getId(), Status.CLOSED));
    }

    @Test
    public void fullLifecycleSetsClosedAt() {
        Ticket t = service.createTicket("Laptop slow", "", Priority.MEDIUM);
        service.assign(t.getId(), "Ravi");
        service.changeStatus(t.getId(), Status.RESOLVED);
        now.set(now.get().plusHours(3));
        service.changeStatus(t.getId(), Status.CLOSED);

        Ticket saved = service.getTicket(t.getId());
        assertEquals(Status.CLOSED, saved.getStatus());
        assertEquals(now.get(), saved.getClosedAt());
    }

    @Test
    public void closedTicketCannotChangeOrBeAssigned() {
        Ticket t = service.createTicket("Laptop slow", "", Priority.MEDIUM);
        service.assign(t.getId(), "Ravi");
        service.changeStatus(t.getId(), Status.RESOLVED);
        service.changeStatus(t.getId(), Status.CLOSED);

        assertThrows(IllegalStateException.class, () -> service.changeStatus(t.getId(), Status.OPEN));
        assertThrows(IllegalStateException.class, () -> service.assign(t.getId(), "Anita"));
    }

    @Test
    public void unknownTicketThrowsNotFound() {
        assertThrows(TicketNotFoundException.class, () -> service.getTicket(99));
        assertThrows(TicketNotFoundException.class, () -> service.assign(99, "Anita"));
        assertThrows(TicketNotFoundException.class, () -> service.deleteTicket(99));
    }

    @Test
    public void deleteRemovesTicket() {
        Ticket t = service.createTicket("Duplicate", "", Priority.LOW);
        service.deleteTicket(t.getId());
        assertTrue(service.listAll().isEmpty());
    }

    @Test
    public void overdueOnlyIncludesUnresolvedTicketsPastDueDate() {
        Ticket critical = service.createTicket("Server down", "", Priority.CRITICAL); // due +4h
        Ticket low = service.createTicket("New mouse", "", Priority.LOW);              // due +72h
        assertTrue(service.overdueTickets().isEmpty());

        now.set(now.get().plusHours(5));
        List<Ticket> overdue = service.overdueTickets();
        assertEquals(1, overdue.size());
        assertEquals(critical.getId(), overdue.get(0).getId());

        // once resolved, the ticket is no longer overdue
        service.assign(critical.getId(), "Anita");
        service.changeStatus(critical.getId(), Status.RESOLVED);
        assertFalse(service.overdueTickets().contains(critical));
        assertFalse(service.overdueTickets().contains(low));
    }

    @Test
    public void reportsCountStatusesAgentsAndResolutionTime() {
        Ticket a = service.createTicket("A", "", Priority.LOW);
        Ticket b = service.createTicket("B", "", Priority.LOW);
        service.createTicket("C", "", Priority.LOW);
        service.assign(a.getId(), "Anita");
        service.assign(b.getId(), "Anita");

        // close ticket A after 90 minutes
        service.changeStatus(a.getId(), Status.RESOLVED);
        now.set(now.get().plusMinutes(90));
        service.changeStatus(a.getId(), Status.CLOSED);

        assertEquals(Integer.valueOf(1), service.statusReport().get(Status.OPEN));
        assertEquals(Integer.valueOf(1), service.statusReport().get(Status.IN_PROGRESS));
        assertEquals(Integer.valueOf(1), service.statusReport().get(Status.CLOSED));
        assertEquals(Integer.valueOf(0), service.statusReport().get(Status.RESOLVED));
        assertEquals(Integer.valueOf(2), service.agentWorkload().get("Anita"));

        OptionalDouble avg = service.averageResolutionMinutes();
        assertTrue(avg.isPresent());
        assertEquals(90.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void averageResolutionIsEmptyWithoutClosedTickets() {
        service.createTicket("Open one", "", Priority.LOW);
        assertFalse(service.averageResolutionMinutes().isPresent());
        assertNotNull(service.statusReport());
    }

    @Test
    public void priorityAndStatusParsingIsForgiving() {
        assertEquals(Priority.HIGH, Priority.fromString(" high "));
        assertEquals(Status.IN_PROGRESS, Status.fromString("in progress"));
        assertThrows(IllegalArgumentException.class, () -> Priority.fromString("urgent"));
        assertThrows(IllegalArgumentException.class, () -> Status.fromString(""));
    }
}
