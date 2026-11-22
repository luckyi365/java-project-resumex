package com.resumex.service;

/**
 * Application-specific exception for all ResumeX operations.
 * <p>
 * Wraps lower-level exceptions (IO, SQL, JSON, HTTP) with a
 * user-friendly message and preserves the original cause.
 */
public class ResumeXException extends Exception {

    /**
     * Creates a new exception with a descriptive message.
     *
     * @param message user-friendly error description
     */
    public ResumeXException(String message) {
        super(message);
    }

    /**
     * Creates a new exception wrapping a root cause.
     *
     * @param message user-friendly error description
     * @param cause   the underlying exception
     */
    public ResumeXException(String message, Throwable cause) {
        super(message, cause);
    }
}
