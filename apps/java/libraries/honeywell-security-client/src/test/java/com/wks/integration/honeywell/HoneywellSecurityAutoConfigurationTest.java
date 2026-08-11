package com.wks.integration.honeywell;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class HoneywellSecurityAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HoneywellSecurityAutoConfiguration.class));

    @Test void endpointAbsentDoesNotCreateClientOrFail() {
        runner.run(context -> { assertThat(context).hasNotFailed(); assertThat(context).doesNotHaveBean(SecurityManagementClient.class); });
    }
    @Test void validHttpsCreatesClientWithoutContactingServer() {
        runner.withPropertyValues("honeywell.security-management.endpoint=https://localhost:9443/SecurityMgmt.svc")
                .run(context -> { assertThat(context).hasNotFailed(); assertThat(context).hasSingleBean(SecurityManagementClient.class); });
    }
    @Test void oidcRequiresAnExplicitTokenProvider() {
        runner.withPropertyValues("honeywell.security-management.endpoint=https://localhost/service",
                        "honeywell.security-management.authentication-type=oidc")
                .run(context -> assertThat(context).hasFailed());
    }
    @Test void oidcWithProviderCreatesClientWithoutResolvingTokenAtStartup() {
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        runner.withBean(HoneywellAccessTokenProvider.class, () -> () -> { calls.incrementAndGet(); return "test-token"; })
                .withPropertyValues("honeywell.security-management.endpoint=https://localhost/service",
                        "honeywell.security-management.authentication-type=oidc")
                .run(context -> { assertThat(context).hasNotFailed(); assertThat(context).hasSingleBean(SecurityManagementClient.class); });
        assertThat(calls).hasValue(0);
    }
    @Test void invalidEndpointsFailClearly() {
        runner.withPropertyValues("honeywell.security-management.endpoint=http://localhost/service")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("honeywell.security-management.endpoint=not a uri")
                .run(context -> assertThat(context).hasFailed());
        HoneywellSecurityProperties p = valid(); p.setEndpoint(" "); assertThatFailure(p, "endpoint");
    }
    @Test void nonPositiveLimitsFail() {
        HoneywellSecurityProperties p = valid(); p.setConnectTimeout(Duration.ZERO); assertThatFailure(p, "connect-timeout");
        p = valid(); p.setReceiveTimeout(Duration.ofSeconds(-1)); assertThatFailure(p, "receive-timeout");
        p = valid(); p.setMaxResponseBytes(0); assertThatFailure(p, "max-response-bytes");
    }
    private static HoneywellSecurityProperties valid() { HoneywellSecurityProperties p = new HoneywellSecurityProperties(); p.setEndpoint("https://localhost/service"); return p; }
    private static void assertThatFailure(HoneywellSecurityProperties p, String text) {
        org.assertj.core.api.Assertions.assertThatThrownBy(p::validatedEndpoint).isInstanceOf(IllegalStateException.class).hasMessageContaining(text);
    }
}
