package com.wks.integration.honeywell;

/** Supplies the current token for one outbound Honeywell request; acquisition and caching belong behind this boundary. */
@FunctionalInterface
public interface HoneywellAccessTokenProvider {
    String getAccessToken();
}
