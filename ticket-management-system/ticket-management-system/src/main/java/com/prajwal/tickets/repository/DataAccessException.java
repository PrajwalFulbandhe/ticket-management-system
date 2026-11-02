package com.prajwal.tickets.repository;

/** Wraps low-level SQLExceptions so the rest of the app does not depend on JDBC. */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
