-- Reporting queries used by the app's "Reports" menu, plus a few extras.
-- Run:  mysql -u root -p ticketdb < db/reports.sql

-- 1. Tickets by status
SELECT status, COUNT(*) AS total
FROM tickets
GROUP BY status
ORDER BY total DESC;

-- 2. Workload per agent (busiest first)
SELECT assigned_to AS agent, COUNT(*) AS total
FROM tickets
WHERE assigned_to IS NOT NULL
GROUP BY assigned_to
ORDER BY total DESC, assigned_to;

-- 3. Overdue tickets: still OPEN or IN_PROGRESS after the SLA due date
SELECT id, title, priority, assigned_to, due_at
FROM tickets
WHERE status IN ('OPEN', 'IN_PROGRESS')
  AND due_at < NOW()
ORDER BY due_at;

-- 4. Average resolution time (minutes) per priority, for closed tickets
SELECT priority,
       COUNT(*) AS closed_tickets,
       ROUND(AVG(TIMESTAMPDIFF(MINUTE, created_at, closed_at)), 1) AS avg_minutes
FROM tickets
WHERE status = 'CLOSED' AND closed_at IS NOT NULL
GROUP BY priority
ORDER BY avg_minutes;

-- 5. SLA compliance: closed tickets that were closed after their due date
SELECT COUNT(*) AS closed_total,
       SUM(CASE WHEN closed_at > due_at THEN 1 ELSE 0 END) AS breached_sla,
       ROUND(100 * SUM(CASE WHEN closed_at <= due_at THEN 1 ELSE 0 END) / COUNT(*), 1) AS sla_met_percent
FROM tickets
WHERE status = 'CLOSED' AND closed_at IS NOT NULL;
