package com.wks.caseengine.rest.honeywell;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import com.wks.integration.honeywell.SecurityManagementClient;
import com.wks.integration.honeywell.SecurityManagementModels.UserAssignedOperation;
import com.wks.integration.honeywell.SecurityManagementModels.UserAssignedOperationsRequest;

@Service
public class HoneywellCaseSecurityService {
    private static final Logger LOG = LoggerFactory.getLogger(HoneywellCaseSecurityService.class);
    private static final String OPERATION = "GetUserAssignedOperationsByScope";
    private final ObjectProvider<SecurityManagementClient> clientProvider;

    public HoneywellCaseSecurityService(ObjectProvider<SecurityManagementClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    public HoneywellSecurityResult getCurrentUserOperations() {
        long started = System.nanoTime();
        LOG.debug("Honeywell stage=HONEYWELL_ENRICHMENT_START operation={}", OPERATION);
        LOG.debug("Honeywell stage=HONEYWELL_REQUEST_BUILD operation={}", OPERATION);
        SecurityManagementClient client = clientProvider.getIfAvailable();
        if (client == null) {
            throw new HoneywellSecurityNotConfiguredException();
        }
        List<UserAssignedOperation> operations = client.getUserAssignedOperationsByScope(new UserAssignedOperationsRequest());
        LOG.debug("Honeywell stage=HONEYWELL_ENRICHMENT_SUCCESS operation={} durationMs={}", OPERATION,
                (System.nanoTime() - started) / 1_000_000);
        return new HoneywellSecurityResult(OPERATION, operations);
    }

    public record HoneywellSecurityResult(String operation, List<UserAssignedOperation> operations) {
        public HoneywellSecurityResult { operations = List.copyOf(operations); }
    }
}
