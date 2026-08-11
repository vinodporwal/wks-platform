package com.wks.integration.honeywell;

public final class SecurityManagementException extends RuntimeException {
    public enum Category { SOAP_FAULT, AUTHENTICATION, AUTHORIZATION, TIMEOUT, CONNECTION, TLS, INVALID_RESPONSE, UNEXPECTED }
    private final Category category;

    public SecurityManagementException(Category category, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
    }

    public Category getCategory() { return category; }
}
