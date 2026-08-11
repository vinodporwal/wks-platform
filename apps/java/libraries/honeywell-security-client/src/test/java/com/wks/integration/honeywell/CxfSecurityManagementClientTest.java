package com.wks.integration.honeywell;

import static com.wks.integration.honeywell.SecurityManagementException.Category.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import javax.net.ssl.SSLException;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.transport.http.HTTPException;
import org.junit.jupiter.api.Test;
import com.wks.integration.honeywell.generated.core.ArrayOfSecScopeDetail;
import com.wks.integration.honeywell.generated.core.SecScopeDetail;
import com.wks.integration.honeywell.generated.security.SecurityMgmtContract;
import com.wks.integration.honeywell.generated.security.SecurityMgmtContractGetUserAssignedOperationsByScopeUSOFaultContractFaultFaultMessage;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.ws.soap.SOAPFaultException;

class CxfSecurityManagementClientTest {
    @Test void classifiesKnownFailuresInSafeOrder() {
        assertThat(CxfSecurityManagementClient.classify(new SecurityMgmtContractGetUserAssignedOperationsByScopeUSOFaultContractFaultFaultMessage())).isEqualTo(SOAP_FAULT);
        assertThat(CxfSecurityManagementClient.classify(new SOAPFaultException(mock(SOAPFault.class)))).isEqualTo(SOAP_FAULT);
        assertThat(CxfSecurityManagementClient.classify(new SocketTimeoutException())).isEqualTo(TIMEOUT);
        assertThat(CxfSecurityManagementClient.classify(new ConnectException())).isEqualTo(CONNECTION);
        assertThat(CxfSecurityManagementClient.classify(new SSLException("TLS"))).isEqualTo(TLS);
        assertThat(CxfSecurityManagementClient.classify(http(401))).isEqualTo(AUTHENTICATION);
        assertThat(CxfSecurityManagementClient.classify(http(403))).isEqualTo(AUTHORIZATION);
        assertThat(CxfSecurityManagementClient.classify(http(503))).isEqualTo(CONNECTION);
        assertThat(CxfSecurityManagementClient.classify(new RuntimeException())).isEqualTo(UNEXPECTED);
        assertThat(CxfSecurityManagementClient.classify(new ResponseSizeLimitInterceptor.ResponseTooLargeException(1))).isEqualTo(INVALID_RESPONSE);
    }

    @Test void mapsGeneratedScopeResponseAndHandlesEmptyArrays() {
        SecurityMgmtContract port = mock(SecurityMgmtContract.class); Client client = mock(Client.class);
        ArrayOfSecScopeDetail response = new ArrayOfSecScopeDetail(); SecScopeDetail scope = new SecScopeDetail();
        scope.setName("scope"); scope.setTypeName("type"); response.getSecScopeDetail().add(scope);
        when(port.getAllScopes()).thenReturn(response);
        CxfSecurityManagementClient subject = new CxfSecurityManagementClient(port, client);
        assertThat(subject.getAllScopes()).containsExactly(new SecurityManagementModels.ScopeInfo("scope", "type", null));
        when(port.getAllScopes()).thenReturn(new ArrayOfSecScopeDetail());
        assertThat(subject.getAllScopes()).isEmpty();
        subject.close(); verify(client).destroy();
    }

    @Test void nullSoapResponseIsInvalid() {
        SecurityMgmtContract port = mock(SecurityMgmtContract.class); when(port.getAllScopes()).thenReturn(null);
        CxfSecurityManagementClient subject = new CxfSecurityManagementClient(port, mock(Client.class));
        assertThatThrownBy(subject::getAllScopes).isInstanceOf(SecurityManagementException.class)
                .extracting("category").isEqualTo(INVALID_RESPONSE);
    }

    private static HTTPException http(int status) { HTTPException value = mock(HTTPException.class); when(value.getResponseCode()).thenReturn(status); return value; }
}
