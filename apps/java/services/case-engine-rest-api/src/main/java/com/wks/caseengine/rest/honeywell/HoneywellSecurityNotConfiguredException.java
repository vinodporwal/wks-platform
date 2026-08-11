package com.wks.caseengine.rest.honeywell;

public final class HoneywellSecurityNotConfiguredException extends RuntimeException {
    public static final String MESSAGE = "Honeywell security integration is not configured";

    public HoneywellSecurityNotConfiguredException() {
        super(MESSAGE);
    }
}
