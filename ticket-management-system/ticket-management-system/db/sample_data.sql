-- Sample tickets for trying out the reports.  Run after schema.sql:
--   mysql -u root -p ticketdb < db/sample_data.sql
-- (due_at = created_at + the SLA hours of the priority: LOW 72h, MEDIUM 24h, HIGH 8h, CRITICAL 4h)

INSERT INTO tickets (title, description, priority, status, assigned_to, created_at, due_at, closed_at) VALUES
('Cannot log in to VPN',      'Timeout error on connect',        'HIGH',     'CLOSED',      'Anita', '2026-10-01 09:00:00', '2026-10-01 17:00:00', '2026-10-01 11:30:00'),
('Outlook keeps crashing',    'Crashes when opening attachments','MEDIUM',   'CLOSED',      'Ravi',  '2026-10-02 10:00:00', '2026-10-03 10:00:00', '2026-10-02 16:00:00'),
('Request for new laptop',    'Replacement for old device',      'LOW',      'RESOLVED',    'Anita', '2026-10-05 11:00:00', '2026-10-08 11:00:00', NULL),
('Payroll portal is down',    'HTTP 503 for all users',          'CRITICAL', 'CLOSED',      'Meena', '2026-10-06 08:00:00', '2026-10-06 12:00:00', '2026-10-06 13:15:00'),
('Shared drive access',       'Needs access to Finance folder',  'MEDIUM',   'IN_PROGRESS', 'Ravi',  '2026-10-10 09:30:00', '2026-10-11 09:30:00', NULL),
('Printer on floor 3 offline','Shows error 79',                  'LOW',      'OPEN',        NULL,    '2026-10-12 14:00:00', '2026-10-15 14:00:00', NULL),
('Email not syncing on phone','Stopped after password change',   'HIGH',     'IN_PROGRESS', 'Anita', '2026-10-14 15:00:00', '2026-10-14 23:00:00', NULL),
('Software install request',  'Needs Python 3.12',               'LOW',      'CLOSED',      'Meena', '2026-10-15 10:00:00', '2026-10-18 10:00:00', '2026-10-16 09:00:00');
