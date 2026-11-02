package com.prajwal.tickets.repository;

import com.prajwal.tickets.model.Priority;
import com.prajwal.tickets.model.Status;
import com.prajwal.tickets.model.Ticket;
import com.prajwal.tickets.util.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * MySQL implementation of TicketRepository using plain JDBC.
 * Every query uses a PreparedStatement (safe against SQL injection) and
 * try-with-resources (connections are always closed).
 */
public class JdbcTicketRepository implements TicketRepository {

    private static final String COLUMNS =
            "id, title, description, priority, status, assigned_to, created_at, due_at, closed_at";

    @Override
    public Ticket save(Ticket ticket) {
        String sql = "INSERT INTO tickets (title, description, priority, status, assigned_to, "
                + "created_at, due_at, closed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ticket.getTitle());
            ps.setString(2, ticket.getDescription());
            ps.setString(3, ticket.getPriority().name());
            ps.setString(4, ticket.getStatus().name());
            ps.setString(5, ticket.getAssignedTo());
            ps.setTimestamp(6, Timestamp.valueOf(ticket.getCreatedAt()));
            ps.setTimestamp(7, Timestamp.valueOf(ticket.getDueAt()));
            ps.setTimestamp(8, ticket.getClosedAt() == null ? null : Timestamp.valueOf(ticket.getClosedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    ticket.setId(keys.getInt(1));
                }
            }
            return ticket;
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the ticket: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Ticket> findById(int id) {
        String sql = "SELECT " + COLUMNS + " FROM tickets WHERE id = ?";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read ticket #" + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Ticket> findAll() {
        return query("SELECT " + COLUMNS + " FROM tickets ORDER BY id");
    }

    @Override
    public List<Ticket> findByStatus(Status status) {
        return query("SELECT " + COLUMNS + " FROM tickets WHERE status = ? ORDER BY id", status.name());
    }

    @Override
    public void update(Ticket ticket) {
        String sql = "UPDATE tickets SET title = ?, description = ?, priority = ?, status = ?, "
                + "assigned_to = ?, due_at = ?, closed_at = ? WHERE id = ?";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ticket.getTitle());
            ps.setString(2, ticket.getDescription());
            ps.setString(3, ticket.getPriority().name());
            ps.setString(4, ticket.getStatus().name());
            ps.setString(5, ticket.getAssignedTo());
            ps.setTimestamp(6, Timestamp.valueOf(ticket.getDueAt()));
            ps.setTimestamp(7, ticket.getClosedAt() == null ? null : Timestamp.valueOf(ticket.getClosedAt()));
            ps.setInt(8, ticket.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update ticket #" + ticket.getId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM tickets WHERE id = ?";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete ticket #" + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Ticket> findOverdue(LocalDateTime now) {
        String sql = "SELECT " + COLUMNS + " FROM tickets "
                + "WHERE status IN ('OPEN', 'IN_PROGRESS') AND due_at < ? ORDER BY due_at";
        List<Ticket> result = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(now));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not read overdue tickets: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<Status, Integer> countByStatus() {
        Map<Status, Integer> counts = new EnumMap<>(Status.class);
        for (Status s : Status.values()) {
            counts.put(s, 0);
        }
        String sql = "SELECT status, COUNT(*) AS total FROM tickets GROUP BY status";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(Status.valueOf(rs.getString("status")), rs.getInt("total"));
            }
            return counts;
        } catch (SQLException e) {
            throw new DataAccessException("Could not count tickets by status: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Integer> countByAgent() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String sql = "SELECT assigned_to, COUNT(*) AS total FROM tickets "
                + "WHERE assigned_to IS NOT NULL GROUP BY assigned_to ORDER BY total DESC, assigned_to";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getString("assigned_to"), rs.getInt("total"));
            }
            return counts;
        } catch (SQLException e) {
            throw new DataAccessException("Could not count tickets by agent: " + e.getMessage(), e);
        }
    }

    @Override
    public OptionalDouble averageResolutionMinutes() {
        String sql = "SELECT AVG(TIMESTAMPDIFF(MINUTE, created_at, closed_at)) AS avg_minutes "
                + "FROM tickets WHERE status = 'CLOSED' AND closed_at IS NOT NULL";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                double avg = rs.getDouble("avg_minutes");
                return rs.wasNull() ? OptionalDouble.empty() : OptionalDouble.of(avg);
            }
            return OptionalDouble.empty();
        } catch (SQLException e) {
            throw new DataAccessException("Could not calculate resolution time: " + e.getMessage(), e);
        }
    }

    // ---- helpers ----------------------------------------------------------

    /** Runs a SELECT that returns tickets; {@code params} are bound as strings. */
    private List<Ticket> query(String sql, String... params) {
        List<Ticket> result = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not read tickets: " + e.getMessage(), e);
        }
    }

    /** Converts the current row of a ResultSet into a Ticket. */
    private Ticket mapRow(ResultSet rs) throws SQLException {
        Timestamp closed = rs.getTimestamp("closed_at");
        String description = rs.getString("description");
        return new Ticket(
                rs.getInt("id"),
                rs.getString("title"),
                description == null ? "" : description,
                Priority.valueOf(rs.getString("priority")),
                Status.valueOf(rs.getString("status")),
                rs.getString("assigned_to"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("due_at").toLocalDateTime(),
                closed == null ? null : closed.toLocalDateTime());
    }
}
