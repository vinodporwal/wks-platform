package com.wks.integration.honeywell;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/** Adds one request-local Bearer header immediately before CXF sends the HTTP request. */
final class HoneywellBearerTokenInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final String AUTHORIZATION = "Authorization";
    private final HoneywellAccessTokenProvider tokenProvider;

    HoneywellBearerTokenInterceptor(HoneywellAccessTokenProvider tokenProvider) {
        super(Phase.PREPARE_SEND);
        this.tokenProvider = tokenProvider;
    }

    @Override public void handleMessage(Message message) throws Fault {
        String token = tokenProvider.getAccessToken();
        if (token == null || token.isBlank()) {
            throw new Fault(new SecurityManagementException(SecurityManagementException.Category.AUTHENTICATION,
                    "Honeywell OIDC access token is missing", null));
        }
        Map<String, List<String>> existing = CastUtils.cast((Map<?, ?>) message.get(Message.PROTOCOL_HEADERS));
        Map<String, List<String>> requestHeaders = existing == null ? new LinkedHashMap<>() : new LinkedHashMap<>(existing);
        requestHeaders.put(AUTHORIZATION, new ArrayList<>(List.of("Bearer " + token)));
        message.put(Message.PROTOCOL_HEADERS, requestHeaders);
    }
}
