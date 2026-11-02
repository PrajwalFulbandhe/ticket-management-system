package com.prajwal.tickets;

import com.prajwal.tickets.model.Priority;
import com.prajwal.tickets.model.Status;
import com.prajwal.tickets.model.Ticket;
import com.prajwal.tickets.repository.DataAccessException;
import com.prajwal.tickets.repository.InMemoryTicketRepository;
import com.prajwal.tickets.repository.JdbcTicketRepository;
import com.prajwal.tickets.repository.TicketRepository;
import com.prajwal.tickets.service.TicketNotFoundException;
import com.prajwal.tickets.service.TicketService;
import com.prajwal.tickets.util.Db;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Scanner;

/** Console menu for the Support Ticket Management System. */
public class App {

    public static void main(String[] args) {
        TicketRepository repository;
        if (Db.isConfigured()) {
            repository = new JdbcTicketRepository();
            System.out.println("Using the database from DB_URL.");
        } else {
            repository = new InMemoryTicketRepository();
            System.out.println("DB_URL is not set -> DEMO MODE (data is kept in memory and lost on exit).");
        }
        TicketService service = new TicketService(repository);

        if (!Db.isConfigured()) {
            seedDemoData(service);
        }
        run(service, new Scanner(System.in));
    }

    private static void run(TicketService service, Scanner in) {
        while (true) {
            printMenu();
            if (!in.hasNextLine()) {
                return; // input finished (for example when piped from a file)
            }
            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> createTicket(service, in);
                    case "2" -> printTickets(service.listAll());
                    case "3" -> System.out.println(service.getTicket(readId(in)).toDetails());
                    case "4" -> assignTicket(service, in);
                    case "5" -> changeStatus(service, in);
                    case "6" -> deleteTicket(service, in);
                    case "7" -> printTickets(service.overdueTickets());
                    case "8" -> printReports(service);
                    case "0" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Please choose a number from the menu.");
                }
            } catch (IllegalArgumentException | IllegalStateException
                     | TicketNotFoundException | DataAccessException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=== Support Ticket Management ===");
        System.out.println("1. Create ticket");
        System.out.println("2. List all tickets");
        System.out.println("3. View ticket details");
        System.out.println("4. Assign ticket to an agent");
        System.out.println("5. Change ticket status");
        System.out.println("6. Delete ticket");
        System.out.println("7. Show overdue tickets (SLA breached)");
        System.out.println("8. Reports");
        System.out.println("0. Exit");
        System.out.print("Choose: ");
    }

    private static void createTicket(TicketService service, Scanner in) {
        System.out.print("Title: ");
        String title = in.nextLine();
        System.out.print("Description: ");
        String description = in.nextLine();
        System.out.print("Priority (LOW, MEDIUM, HIGH, CRITICAL): ");
        Priority priority = Priority.fromString(in.nextLine());
        Ticket ticket = service.createTicket(title, description, priority);
        System.out.println("Created ticket #" + ticket.getId() + " - due "
                + ticket.getDueAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
    }

    private static void assignTicket(TicketService service, Scanner in) {
        int id = readId(in);
        System.out.print("Agent name: ");
        Ticket ticket = service.assign(id, in.nextLine());
        System.out.println("Ticket #" + ticket.getId() + " assigned to " + ticket.getAssignedTo()
                + " (status: " + ticket.getStatus() + ")");
    }

    private static void changeStatus(TicketService service, Scanner in) {
        int id = readId(in);
        System.out.print("New status (OPEN, IN_PROGRESS, RESOLVED, CLOSED): ");
        Ticket ticket = service.changeStatus(id, Status.fromString(in.nextLine()));
        System.out.println("Ticket #" + ticket.getId() + " is now " + ticket.getStatus());
    }

    private static void deleteTicket(TicketService service, Scanner in) {
        int id = readId(in);
        service.deleteTicket(id);
        System.out.println("Ticket #" + id + " deleted.");
    }

    private static void printTickets(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            System.out.println("No tickets to show.");
            return;
        }
        tickets.forEach(System.out::println);
    }

    private static void printReports(TicketService service) {
        System.out.println("--- Tickets by status ---");
        for (Map.Entry<Status, Integer> e : service.statusReport().entrySet()) {
            System.out.printf("  %-12s %d%n", e.getKey(), e.getValue());
        }
        System.out.println("--- Workload per agent ---");
        Map<String, Integer> workload = service.agentWorkload();
        if (workload.isEmpty()) {
            System.out.println("  No assigned tickets.");
        }
        workload.forEach((agent, count) -> System.out.printf("  %-12s %d%n", agent, count));
        System.out.println("--- Average resolution time ---");
        OptionalDouble avg = service.averageResolutionMinutes();
        if (avg.isPresent()) {
            long minutes = Math.round(avg.getAsDouble());
            System.out.println("  " + (minutes / 60) + " h " + (minutes % 60) + " min");
        } else {
            System.out.println("  No closed tickets yet.");
        }
    }

    private static int readId(Scanner in) {
        System.out.print("Ticket id: ");
        try {
            return Integer.parseInt(in.nextLine().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid ticket number.");
        }
    }

    /** A few sample tickets so demo mode is not empty. */
    private static void seedDemoData(TicketService service) {
        service.createTicket("Cannot log in to VPN", "Timeout error on connect", Priority.HIGH);
        service.createTicket("Outlook keeps crashing", "Crashes when opening attachments", Priority.MEDIUM);
        service.createTicket("Request for new laptop", "Replacement for old device", Priority.LOW);
        service.assign(1, "Anita");
    }
}
