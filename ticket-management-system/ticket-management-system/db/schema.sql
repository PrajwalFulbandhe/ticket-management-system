-- Support Ticket Management System - MySQL 8.0.16+ (CHECK constraints are enforced from 8.0.16)
-- Run:  mysql -u root -p < db/schema.sql

CREATE DATABASE IF NOT EXISTS ticketdb;
USE ticketdb;

DROP TABLE IF EXISTS tickets;

CREATE TABLE tickets (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(100) NOT NULL,
    description TEXT,
    priority    VARCHAR(10)  NOT NULL,
    status      VARCHAR(12)  NOT NULL DEFAULT 'OPEN',
    assigned_to VARCHAR(50),
    created_at  DATETIME     NOT NULL,
    due_at      DATETIME     NOT NULL,
    closed_at   DATETIME     NULL,
    CONSTRAINT chk_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_status   CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'))
);

-- Speeds up the status filter and the overdue report
CREATE INDEX idx_tickets_status_due ON tickets (status, due_at);
CREATE INDEX idx_tickets_assigned   ON tickets (assigned_to);
