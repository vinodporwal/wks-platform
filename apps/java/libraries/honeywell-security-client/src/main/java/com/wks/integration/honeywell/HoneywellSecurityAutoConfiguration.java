package com.wks.integration.honeywell;

import java.net.URL;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.wks.integration.honeywell.generated.security.SecurityMgmt;
import com.wks.integration.honeywell.generated.security.SecurityMgmtContract;
import jakarta.xml.ws.BindingProvider;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HoneywellSecurityProperties.class)
public class HoneywellSecurityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(HoneywellTransportConfigurer.class)
    @ConditionalOnProperty(prefix = "honeywell.security-management", name = "authentication-type", havingValue = "none", matchIfMissing = true)
    HoneywellTransportConfigurer honeywellTransportConfigurer() { return client -> { }; }

    @Bean
    @ConditionalOnMissingBean(HoneywellTransportConfigurer.class)
    @ConditionalOnProperty(prefix = "honeywell.security-management", name = "authentication-type", havingValue = "oidc")
    HoneywellTransportConfigurer oidcHoneywellTransportConfigurer(HoneywellAccessTokenProvider tokenProvider) {
        return new OidcHoneywellTransportConfigurer(tokenProvider);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnExpression("'${honeywell.security-management.endpoint:}' != ''")
    SecurityManagementClient securityManagementClient(HoneywellSecurityProperties properties,
            HoneywellTransportConfigurer transportConfigurer) {
        String endpoint = properties.validatedEndpoint().toString();
        URL wsdl = HoneywellSecurityAutoConfiguration.class.getResource("/wsdl/honeywell/SecurityMgmtRuntime.wsdl");
        if (wsdl == null) throw new IllegalStateException("Bundled Honeywell runtime WSDL is missing");
        SecurityMgmtContract port = new SecurityMgmt(wsdl).getBasicHttpBindingSecurityMgmtContract();
        ((BindingProvider) port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        Client client = ClientProxy.getClient(port);
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setConnectionTimeout(properties.getConnectTimeout().toMillis());
        policy.setReceiveTimeout(properties.getReceiveTimeout().toMillis());
        ((HTTPConduit) client.getConduit()).setClient(policy);
        client.getInInterceptors().add(new ResponseSizeLimitInterceptor(properties.getMaxResponseBytes()));
        transportConfigurer.configure(client);
        return new CxfSecurityManagementClient(port, client);
    }

}
