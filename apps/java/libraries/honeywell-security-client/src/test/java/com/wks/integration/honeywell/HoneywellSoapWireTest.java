package com.wks.integration.honeywell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.cxf.frontend.ClientProxy;
import com.wks.integration.honeywell.generated.core.ModelHierarchyName;
import com.wks.integration.honeywell.generated.security.SecurityMgmt;
import com.wks.integration.honeywell.generated.security.SecurityMgmtContract;
import jakarta.xml.ws.BindingProvider;

class HoneywellSoapWireTest {
    @Test void completeBindingSendsSoap11ActionsAndHonorsEndpointOverride() throws Exception {
        List<Captured> captured = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/override", exchange -> handle(exchange, captured)); server.start();
        try {
            URL wsdl = getClass().getResource("/wsdl/honeywell/SecurityMgmtRuntime.wsdl");
            SecurityMgmtContract port = new SecurityMgmt(wsdl).getBasicHttpBindingSecurityMgmtContract();
            AtomicInteger tokenNumber = new AtomicInteger();
            new OidcHoneywellTransportConfigurer(() -> tokenNumber.incrementAndGet() == 1 ? "token-A" : "token-B")
                    .configure(ClientProxy.getClient(port));
            String endpoint = "http://localhost:" + server.getAddress().getPort() + "/override";
            ((BindingProvider) port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
            port.getAllScopes();
            ModelHierarchyName hierarchy = new ModelHierarchyName(); hierarchy.setModelName("model"); hierarchy.setHierarchyName("hierarchy");
            port.getScopeContexts(hierarchy);
            assertThat(captured).hasSize(2);
            assertWire(captured.get(0), "GetAllScopes");
            assertWire(captured.get(1), "GetScopeContexts");
            assertThat(captured.get(0).authorization()).isEqualTo("Bearer token-A");
            assertThat(captured.get(1).authorization()).isEqualTo("Bearer token-B");
            assertThat(captured).allSatisfy(v -> assertThat(v.body()).doesNotContain("token-A", "token-B", "Authorization"));
            assertThat(captured).allSatisfy(v -> assertThat(v.path()).isEqualTo("/override"));
        } finally { server.stop(0); }
    }

    @Test void missingTokenPreventsAnyOutboundRequest() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/never", exchange -> { requests.incrementAndGet(); exchange.close(); }); server.start();
        try {
            SecurityMgmtContract port = new SecurityMgmt(getClass().getResource("/wsdl/honeywell/SecurityMgmtRuntime.wsdl"))
                    .getBasicHttpBindingSecurityMgmtContract();
            ((BindingProvider) port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                    "http://localhost:" + server.getAddress().getPort() + "/never");
            var client = ClientProxy.getClient(port);
            new OidcHoneywellTransportConfigurer(() -> " ").configure(client);
            CxfSecurityManagementClient subject = new CxfSecurityManagementClient(port, client);
            assertThatThrownBy(subject::getAllScopes).isInstanceOf(SecurityManagementException.class)
                    .extracting("category").isEqualTo(SecurityManagementException.Category.AUTHENTICATION);
            assertThat(requests).hasValue(0);
            subject.close();
        } finally { server.stop(0); }
    }

    private static void assertWire(Captured value, String operation) throws Exception {
        assertThat(value.method()).isEqualTo("POST");
        assertThat(value.contentType()).startsWith("text/xml");
        assertThat(value.body()).contains("http://schemas.xmlsoap.org/soap/envelope/");
        if (operation.equals("GetAllScopes")) assertThat(value.body()).contains("<soap:Body/>"); // WSDL input message has no parts.
        else assertThat(value.body()).contains("ModelHierarchyName").contains("model").contains("hierarchy");
        assertThat(value.soapAction()).isEqualTo('"' + actionFromBinding(operation) + '"');
    }
    private static String actionFromBinding(String operation) throws Exception {
        var factory = DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
        var document = factory.newDocumentBuilder().parse(HoneywellSoapWireTest.class.getResourceAsStream("/wsdl/honeywell/SecurityMgmtService.wsdl"));
        String expression = "//*[local-name()='operation'][@name='" + operation + "']/*[local-name()='operation']/@soapAction";
        return (String) XPathFactory.newInstance().newXPath().evaluate(expression, document, XPathConstants.STRING);
    }
    private static void handle(HttpExchange exchange, List<Captured> captured) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String action = exchange.getRequestHeaders().getFirst("SOAPAction");
        captured.add(new Captured(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst("Content-Type"), action,
                exchange.getRequestHeaders().getFirst("Authorization"), body));
        String element = action.contains("GetAllScopes") ? "SecScope" : "ScopeContextResponseRoles";
        byte[] response = ("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><" + element
                + " xmlns=\"http://Honeywell.MES.Core.Service.SecurityConfig\"/></s:Body></s:Envelope>").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/xml; charset=utf-8"); exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response); exchange.close();
    }
    private record Captured(String method, String path, String contentType, String soapAction, String authorization, String body) { }
}
