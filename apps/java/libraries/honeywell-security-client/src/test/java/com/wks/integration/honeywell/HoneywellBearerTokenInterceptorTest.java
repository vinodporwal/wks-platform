package com.wks.integration.honeywell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.Test;

class HoneywellBearerTokenInterceptorTest {
    @Test void missingTokensFailAsAuthenticationBeforeTransport() {
        for (String token : new String[] { null, "", "  " }) {
            Message message = new MessageImpl();
            HoneywellBearerTokenInterceptor interceptor = new HoneywellBearerTokenInterceptor(() -> token);
            assertThatThrownBy(() -> interceptor.handleMessage(message)).isInstanceOf(Fault.class)
                    .hasRootCauseInstanceOf(SecurityManagementException.class)
                    .rootCause().extracting("category").isEqualTo(SecurityManagementException.Category.AUTHENTICATION);
            assertThat(message.get(Message.PROTOCOL_HEADERS)).isNull();
        }
    }

    @Test void concurrentMessagesNeverShareAuthorizationMapsOrTokens() throws Exception {
        HoneywellBearerTokenInterceptor interceptor = new HoneywellBearerTokenInterceptor(() -> Thread.currentThread().getName());
        Callable<Message> request = () -> { Message message = new MessageImpl(); interceptor.handleMessage(message); return message; };
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Message> messages = executor.invokeAll(List.of(request, request)).stream().map(future -> {
                try { return future.get(); } catch (Exception e) { throw new AssertionError(e); }
            }).toList();
            Map<?, ?> first = (Map<?, ?>) messages.get(0).get(Message.PROTOCOL_HEADERS);
            Map<?, ?> second = (Map<?, ?>) messages.get(1).get(Message.PROTOCOL_HEADERS);
            assertThat(first).isNotSameAs(second);
            assertThat(first.get("Authorization")).isNotEqualTo(second.get("Authorization"));
            assertThat(first.toString()).doesNotContain("Basic "); assertThat(second.toString()).doesNotContain("Basic ");
        } finally { executor.shutdownNow(); }
    }
}
