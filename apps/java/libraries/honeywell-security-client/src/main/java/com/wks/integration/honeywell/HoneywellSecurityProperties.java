package com.wks.integration.honeywell;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("honeywell.security-management")
public class HoneywellSecurityProperties {
    public enum AuthenticationType { NONE, OIDC }
    private String endpoint;
    private AuthenticationType authenticationType = AuthenticationType.NONE;
    private Duration connectTimeout = Duration.ofMinutes(5);
    private Duration receiveTimeout = Duration.ofMinutes(10);
    private long maxResponseBytes = 10L * 1024 * 1024;
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public AuthenticationType getAuthenticationType() { return authenticationType; }
    public void setAuthenticationType(AuthenticationType value) { this.authenticationType = value; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration value) { this.connectTimeout = value; }
    public Duration getReceiveTimeout() { return receiveTimeout; }
    public void setReceiveTimeout(Duration value) { this.receiveTimeout = value; }
    public long getMaxResponseBytes() { return maxResponseBytes; }
    public void setMaxResponseBytes(long value) { this.maxResponseBytes = value; }

    URI validatedEndpoint() {
        if (endpoint == null || endpoint.isBlank()) throw new IllegalStateException("honeywell.security-management.endpoint must not be blank");
        final URI uri;
        try { uri = URI.create(endpoint); } catch (IllegalArgumentException e) {
            throw new IllegalStateException("honeywell.security-management.endpoint must be a valid URI", e);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null)
            throw new IllegalStateException("honeywell.security-management.endpoint must be an absolute HTTPS URI");
        if (authenticationType == null) throw new IllegalStateException("honeywell.security-management.authentication-type must not be null");
        positive(connectTimeout, "connect-timeout"); positive(receiveTimeout, "receive-timeout");
        if (maxResponseBytes <= 0) throw new IllegalStateException("honeywell.security-management.max-response-bytes must be greater than zero");
        return uri;
    }
    private static void positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative())
            throw new IllegalStateException("honeywell.security-management." + name + " must be greater than zero");
    }
}
