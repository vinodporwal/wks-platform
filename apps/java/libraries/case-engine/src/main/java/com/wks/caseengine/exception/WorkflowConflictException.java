package com.wks.caseengine.exception;

/**
 * Thrown when an operation would violate a workflow invariant, e.g. starting a
 * second active AOP approval workflow for a plant + year that already has one.
 * Mapped to HTTP 409 Conflict by {@link GlobalExceptionHandler}.
 */
public class WorkflowConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WorkflowConflictException(final String message) {
        super(message);
    }
}
