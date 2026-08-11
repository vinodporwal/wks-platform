package com.wks.integration.honeywell;

import org.apache.cxf.endpoint.Client;

/** Installs dynamic per-request OIDC Bearer authentication without coupling SOAP operations to authentication. */
final class OidcHoneywellTransportConfigurer implements HoneywellTransportConfigurer {
    private final HoneywellAccessTokenProvider tokenProvider;
    OidcHoneywellTransportConfigurer(HoneywellAccessTokenProvider tokenProvider) { this.tokenProvider = tokenProvider; }
    @Override public void configure(Client client) { client.getOutInterceptors().add(new HoneywellBearerTokenInterceptor(tokenProvider)); }
}
