# Support Ticket Management System

A console application written in **Core Java** with a **MySQL** database (JDBC) for logging, assigning and tracking IT support tickets. Every ticket gets an **SLA due date** based on its priority, and SQL reports show workload, overdue tickets and resolution times.

It is modelled on how an IT service desk works: tickets come in, get assigned to an agent, move through a fixed lifecycle, and are measured against their SLA.

## Features

- Create, view, list, assign and delete tickets
- Priorities with SLA hours: `CRITICAL` 4h, `HIGH` 8h, `MEDIUM` 24h, `LOW` 72h (due date is calculated automatically)
- Enforced ticket lifecycle: `OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED` (a closed ticket can never change again)
- Assigning an `OPEN` ticket moves it to `IN_PROGRESS`
- Overdue report: tickets still open after their due date
- Reports: tickets by status, workload per agent, average resolution time
- Input validation with clear error messages
- **Demo mode**: runs without a database, so you can try it in seconds

## Tech stack

| Area | Tools |
|---|---|
| Language | Java 17 (OOP, collections, enums, exception handling) |
| Database | MySQL 8, JDBC, `PreparedStatement` |
| Build | Maven |
| Testing | JUnit 4 (16 tests) |
| Version control | Git / GitHub |

## Project structure

```
db/
  schema.sql          table, constraints and indexes
  sample_data.sql     example tickets
  reports.sql         reporting queries (GROUP BY, AVG, TIMESTAMPDIFF)
src/main/java/com/prajwal/tickets/
  App.java                       console menu
  model/                         Ticket, Priority (SLA hours), Status (lifecycle)
  service/TicketService.java     business rules and validation
  repository/                    TicketRepository (interface)
                                 JdbcTicketRepository (MySQL)
                                 InMemoryTicketRepository (demo + tests)
  util/Db.java                   connection settings from environment variables
src/test/java/.../TicketServiceTest.java
```

## How it works

The code is split into three layers so each part has one job:

1. **App** (user interface) reads input and prints results. It contains no business rules.
2. **TicketService** (business logic) validates input, calculates due dates and decides which status changes are allowed.
3. **TicketRepository** (storage) is an interface. `JdbcTicketRepository` saves to MySQL; `InMemoryTicketRepository` keeps data in memory. Because the service only knows the interface, the same rules run against either one. This is also why the unit tests do not need a database.

Other design points:

- The service receives a clock (`Supplier<LocalDateTime>`) so tests can move time forward and check SLA and overdue logic instantly.
- All SQL uses `PreparedStatement` with bound parameters, which prevents SQL injection.
- Database passwords are read from environment variables, never stored in the code.
- `SQLException` is wrapped in a custom `DataAccessException`, so the rest of the app does not depend on JDBC.

## Run it

### Option 1 - Demo mode (no database needed)

Requires Java 17+ and Maven.

```bash
mvn compile exec:java
```

Without `DB_URL` set, the app starts with a few sample tickets kept in memory.

### Option 2 - With MySQL

1. Create the database and load sample data:

   ```bash
   mysql -u root -p < db/schema.sql
   mysql -u root -p ticketdb < db/sample_data.sql
   ```

2. Set the connection details, then run:

   ```bash
   # Linux / macOS
   export DB_URL="jdbc:mysql://localhost:3306/ticketdb"
   export DB_USER="root"
   export DB_PASSWORD="your_password"
   mvn compile exec:java
   ```

   ```bat
   :: Windows (Command Prompt)
   set DB_URL=jdbc:mysql://localhost:3306/ticketdb
   set DB_USER=root
   set DB_PASSWORD=your_password
   mvn compile exec:java
   ```

3. Try the reporting queries directly in MySQL:

   ```bash
   mysql -u root -p ticketdb < db/reports.sql
   ```

## Run the tests

```bash
mvn test
```

The tests cover SLA due dates, validation, the status lifecycle, overdue detection, and the reports.

## Example session

```
=== Support Ticket Management ===
1. Create ticket
2. List all tickets
...
Choose: 2
#1   HIGH     IN_PROGRESS Cannot log in to VPN           | agent: Anita    | due: 02 Nov 2026 13:13
#2   MEDIUM   OPEN        Outlook keeps crashing         | agent: -        | due: 03 Nov 2026 05:13
```

## Possible improvements

- Add a REST API with Spring Boot on top of `TicketService`
- Add a connection pool (HikariCP) and database integration tests
- Add comments and a history log for each ticket
- Support PostgreSQL (change `AUTO_INCREMENT` to `SERIAL` and `TIMESTAMPDIFF` to `EXTRACT`)

## Author

Prajwal Fulbandhe
