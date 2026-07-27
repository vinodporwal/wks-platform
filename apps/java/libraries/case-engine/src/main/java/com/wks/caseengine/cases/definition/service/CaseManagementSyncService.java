package com.wks.caseengine.cases.definition.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wks.caseengine.rest.db2.entity.Case;
import com.wks.caseengine.rest.db2.entity.CaseCauseCategory;
import com.wks.caseengine.rest.db2.repository.CaseCauseCategoryRepository;
import com.wks.caseengine.rest.model.Attribute;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CaseManagementSyncService {

    private static final String CASE_SOURCE = "XOM";
    private static final String CLOSED_STATUS = "Closed";
    private static final String NOT_CATEGORISED = "Not Categorised";

    private static final String FIND_STATUS_SQL = """
            SELECT Status_Name
            FROM dbo.CaseStates
            WHERE Status_Name = ?
            """;

    private static final String FIND_CATEGORY_SQL = """
            SELECT CaseCategory_PK_ID, Name
            FROM dbo.CaseCategories
            WHERE Name = ?
            """;

    private static final String FIND_CASE_SQL = """
            SELECT Case_PK_ID, Status, Closed_Dt
            FROM dbo.Cases WITH (UPDLOCK, HOLDLOCK)
            WHERE External_Task_Id = ?
            """;

    private static final String INSERT_CASE_SQL = """
            INSERT INTO dbo.Cases
            (
                Case_Name,
                Description,
                Closed_Dt,
                Archieved_DT,
                Status,
                Case_Source,
                Created_By,
                IsArchieved,
                Case_Link,
                Document_Link,
                Archieve_Link,
                External_Task_Id,
                Content,
                CaseCategory_PK_ID
            )
            OUTPUT INSERTED.Case_PK_ID
            VALUES
            (
                ?,
                ?,
                CASE WHEN ? = N'Closed' THEN GETUTCDATE() ELSE NULL END,
                NULL,
                ?,
                ?,
                ?,
                0,
                ?,
                NULL,
                NULL,
                ?,
                ?,
                ?
            )
            """;

    private static final String UPDATE_CASE_SQL = """
            UPDATE dbo.Cases
            SET
                Case_Name = ?,
                Description = ?,
                Closed_Dt =
                    CASE
                        WHEN Status <> N'Closed' AND ? = N'Closed' THEN GETUTCDATE()
                        WHEN Status = N'Closed' AND ? <> N'Closed' THEN NULL
                        ELSE Closed_Dt
                    END,
                Status = ?,
                Case_Source = ?,
                Created_By = ?,
                IsArchieved = 0,
                Case_Link = ?,
                Content = ?,
                CaseCategory_PK_ID = ?
            WHERE External_Task_Id = ?
            """;

    private static final String FIND_FAULT_SQL = """
            SELECT FaultHistory_PK_ID
            FROM dbo.FaultHistory
            WHERE FaultHistoryClusteredId = ?
            """;

    private static final String INSERT_MAPPING_SQL = """
            INSERT INTO dbo.CaseMappings
            (
                Case_PK_ID,
                Fault_PK_ID,
                Mapped_By
            )
            SELECT ?, ?, ?
            WHERE NOT EXISTS
            (
                SELECT 1
                FROM dbo.CaseMappings WITH (UPDLOCK, HOLDLOCK)
                WHERE Case_PK_ID = ?
                  AND Fault_PK_ID = ?
            )
            """;

    private static final String FIND_CASE_CLOSE_OUT_SQL = """
            SELECT CaseCloseOut_PK_ID
            FROM dbo.CaseCloseOuts WITH (UPDLOCK, HOLDLOCK)
            WHERE Case_PK_ID = ?
            """;

    private static final String INSERT_CASE_CLOSE_OUT_SQL = """
            INSERT INTO dbo.CaseCloseOuts
            (
                Case_PK_ID,
                Modified_DT,
                ModifiedUser_PK_ID,
                CloseOut_DT,
                CloseOutUser_PK_ID,
                CaseCategory_PK_ID,
                CaseCause_PK_ID,
                Reason,
                Causes,
                Consequences,
                Recommendations,
                Comments,
                Estimated_Savings,
                Cost
            )
            VALUES
            (
                ?,
                GETUTCDATE(),
                ?,
                GETUTCDATE(),
                ?,
                ?,
                NULL,
                NULL,
                NULL,
                NULL,
                NULL,
                ?,
                0,
                0
            )
            """;

    private final JdbcTemplate db1JdbcTemplate;
    private final CaseCauseCategoryRepository caseCauseCategoryRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate db1TransactionTemplate;

    public CaseManagementSyncService(
            @Qualifier("db1JdbcTemplate") JdbcTemplate db1JdbcTemplate,
            @Qualifier("db1TransactionManager") PlatformTransactionManager db1TransactionManager,
            CaseCauseCategoryRepository caseCauseCategoryRepository,
            ObjectMapper objectMapper) {
        this.db1JdbcTemplate = db1JdbcTemplate;
        this.caseCauseCategoryRepository = caseCauseCategoryRepository;
        this.objectMapper = objectMapper;
        this.db1TransactionTemplate = new TransactionTemplate(db1TransactionManager);
        this.db1TransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void synchronize(Case sourceCase) {
        SourceCaseData sourceData = prepareSourceData(sourceCase);
        log.info("Synchronizing WKS case {}", sourceData.caseNo());

        try {
            db1TransactionTemplate.executeWithoutResult(status -> synchronizeInDb1(sourceData));
        } catch (CaseSynchronizationException e) {
            log.warn("Synchronization failed for WKS case {}", sourceData.caseNo());
            throw e;
        } catch (DataAccessException e) {
            log.error("Synchronization failed for WKS case {}", sourceData.caseNo());
            throw failure(sourceData.caseNo(), "DB1 synchronization failed.", e);
        } catch (RuntimeException e) {
            log.error("Synchronization failed for WKS case {}", sourceData.caseNo());
            throw failure(sourceData.caseNo(), "DB1 synchronization failed.", e);
        }
    }

    private SourceCaseData prepareSourceData(Case sourceCase) {
        if (sourceCase == null) {
            throw new CaseSynchronizationException("Cannot synchronize WKS case: source case is missing.");
        }

        String caseNo = requiredValue(sourceCase.getCaseNo(), 200, "case number", "unknown");
        JsonNode container = readContainer(sourceCase, caseNo);
        String caseName = requiredJsonText(container, "caseTitle", 200, "case title", caseNo);
        String description = optionalJsonText(container, "caseDescription", 500, "case description", caseNo);

        if (sourceCase.getStatus() == null) {
            throw failure(caseNo, "status is missing.");
        }
        String status = requiredValue(sourceCase.getStatus().getName(), 20, "status", caseNo);
        if (sourceCase.getOwner() == null) {
            throw failure(caseNo, "owner is missing.");
        }
        String createdBy = sourceCase.getOwner().getName();
        createdBy = requiredValue(createdBy, 100, "owner name", caseNo);
        if (sourceCase.getOwner().getId() == null) {
            throw failure(caseNo, "owner ID is missing.");
        }
        UUID ownerId;
        try {
            ownerId = UUID.fromString(sourceCase.getOwner().getId().toString().trim());
        } catch (RuntimeException e) {
            throw failure(caseNo, "owner ID is not a valid UUID.", e);
        }
        String caseLink = requiredValue(sourceCase.getCaseUrl(), 500, "case link", caseNo);

        List<Integer> eventIds = normalizeEventIds(sourceCase.getEventIds(), caseNo);

        String content;
        try {
            content = objectMapper.writeValueAsString(sourceCase.getAttributes());
        } catch (JsonProcessingException e) {
            throw failure(caseNo, "case attributes could not be serialized.", e);
        }

        String closeOutComments = closeOutComments(container, caseNo);
        CaseCauseCategory category = resolveSourceCategory(container, caseNo);
        Long caseCauseCategoryId = category.getId();
        String categoryName = requiredValue(
                category.getName(), Integer.MAX_VALUE, "case cause category name", caseNo);

        return new SourceCaseData(
                caseNo,
                caseName,
                description,
                status,
                createdBy,
                ownerId,
                caseLink,
                content,
                caseCauseCategoryId,
                categoryName,
                closeOutComments,
                eventIds);
    }

    private void synchronizeInDb1(SourceCaseData sourceData) {
        validateTargetStatus(sourceData);
        UUID categoryId = resolveTargetCategory(sourceData);
        Map<Integer, UUID> faultIds = resolveFaultIds(sourceData);

        List<TargetCase> targetCases = findTargetCases(sourceData.caseNo());
        if (targetCases.size() > 1) {
            throw failure(sourceData.caseNo(),
                    "multiple target cases use the same external task ID.");
        }

        TargetCase targetCase;
        if (targetCases.isEmpty()) {
            targetCase = insertTargetCase(sourceData, categoryId);
            log.info("Inserted DB1 case for WKS case {}", sourceData.caseNo());
        } else {
            targetCase = targetCases.get(0);
            updateTargetCase(sourceData, categoryId);
            log.info("Updated DB1 case for WKS case {}", sourceData.caseNo());
        }

        int mappingsAdded = insertMissingMappings(targetCase.casePkId(), faultIds, sourceData.createdBy());
        log.info("Added {} case mappings for WKS case {}", mappingsAdded, sourceData.caseNo());

        if (CLOSED_STATUS.equals(sourceData.status())) {
            ensureCaseCloseOut(
                    targetCase.casePkId(),
                    categoryId,
                    sourceData.ownerId(),
                    sourceData.closeOutComments(),
                    sourceData.caseNo());
        }
    }

    private void validateTargetStatus(SourceCaseData sourceData) {
        List<String> statuses = db1JdbcTemplate.query(
                FIND_STATUS_SQL,
                (rs, rowNum) -> rs.getString("Status_Name"),
                sourceData.status());
        if (statuses.size() != 1 || !sourceData.status().equals(statuses.get(0))) {
            throw failure(sourceData.caseNo(), "target status was not found.");
        }
    }

    private UUID resolveTargetCategory(SourceCaseData sourceData) {
        List<TargetCategory> categories = db1JdbcTemplate.query(
                FIND_CATEGORY_SQL,
                (rs, rowNum) -> new TargetCategory(
                        uuid(rs.getString("CaseCategory_PK_ID"), sourceData.caseNo(), "target category ID"),
                        rs.getString("Name")),
                sourceData.categoryName());
        if (categories.isEmpty()) {
            if (NOT_CATEGORISED.equals(sourceData.categoryName())) {
                throw failure(sourceData.caseNo(), "target Not Categorised category was not found.");
            }
            throw failure(sourceData.caseNo(), "target case category was not found for case cause category "
                    + sourceData.caseCauseCategoryId() + ".");
        }
        if (categories.size() > 1) {
            if (NOT_CATEGORISED.equals(sourceData.categoryName())) {
                throw failure(sourceData.caseNo(), "multiple target Not Categorised categories were found.");
            }
            throw failure(sourceData.caseNo(), "multiple target case categories matched case cause category "
                    + sourceData.caseCauseCategoryId() + ".");
        }
        if (!sourceData.categoryName().equals(categories.get(0).name())) {
            if (NOT_CATEGORISED.equals(sourceData.categoryName())) {
                throw failure(sourceData.caseNo(),
                        "returned target category name did not exactly match Not Categorised.");
            }
            throw failure(sourceData.caseNo(), "target case category name did not exactly match case cause category "
                    + sourceData.caseCauseCategoryId() + ".");
        }
        return categories.get(0).categoryPkId();
    }

    private Map<Integer, UUID> resolveFaultIds(SourceCaseData sourceData) {
        Map<Integer, UUID> faultIds = new LinkedHashMap<>();
        for (Integer eventId : sourceData.eventIds()) {
            List<UUID> matches = db1JdbcTemplate.query(
                    FIND_FAULT_SQL,
                    (rs, rowNum) -> uuid(
                            rs.getString("FaultHistory_PK_ID"),
                            sourceData.caseNo(),
                            "FaultHistory ID"),
                    eventId);
            if (matches.size() != 1) {
                throw failure(sourceData.caseNo(),
                        "event " + eventId + " has no unique matching FaultHistory.");
            }
            faultIds.put(eventId, matches.get(0));
        }
        return faultIds;
    }

    private List<TargetCase> findTargetCases(String caseNo) {
        return db1JdbcTemplate.query(
                FIND_CASE_SQL,
                (rs, rowNum) -> new TargetCase(
                        uuid(rs.getString("Case_PK_ID"), caseNo, "target case ID"),
                        rs.getString("Status"),
                        rs.getTimestamp("Closed_Dt")),
                caseNo);
    }

    private TargetCase insertTargetCase(SourceCaseData sourceData, UUID categoryId) {
        try {
            TargetCase inserted = db1JdbcTemplate.queryForObject(
                    INSERT_CASE_SQL,
                    (rs, rowNum) -> new TargetCase(
                            uuid(rs.getString("Case_PK_ID"), sourceData.caseNo(), "target case ID"),
                            sourceData.status(),
                            null),
                    sourceData.caseName(),
                    sourceData.description(),
                    sourceData.status(),
                    sourceData.status(),
                    CASE_SOURCE,
                    sourceData.createdBy(),
                    sourceData.caseLink(),
                    sourceData.caseNo(),
                    sourceData.content(),
                    categoryId);
            if (inserted == null) {
                throw failure(sourceData.caseNo(), "target case insert returned no identifier.");
            }
            return inserted;
        } catch (DuplicateKeyException e) {
            List<TargetCase> concurrentCases = findTargetCases(sourceData.caseNo());
            if (concurrentCases.size() != 1) {
                throw failure(sourceData.caseNo(), "target case correlation is invalid.", e);
            }
            updateTargetCase(sourceData, categoryId);
            return concurrentCases.get(0);
        }
    }

    private void updateTargetCase(SourceCaseData sourceData, UUID categoryId) {
        int updated = db1JdbcTemplate.update(
                UPDATE_CASE_SQL,
                sourceData.caseName(),
                sourceData.description(),
                sourceData.status(),
                sourceData.status(),
                sourceData.status(),
                CASE_SOURCE,
                sourceData.createdBy(),
                sourceData.caseLink(),
                sourceData.content(),
                categoryId,
                sourceData.caseNo());
        if (updated != 1) {
            throw failure(sourceData.caseNo(), "target case update affected an unexpected number of rows.");
        }
    }

    private int insertMissingMappings(UUID casePkId, Map<Integer, UUID> faultIds, String mappedBy) {
        int inserted = 0;
        for (UUID faultPkId : faultIds.values()) {
            inserted += db1JdbcTemplate.update(
                    INSERT_MAPPING_SQL,
                    casePkId,
                    faultPkId,
                    mappedBy,
                    casePkId,
                    faultPkId);
        }
        return inserted;
    }

    private void ensureCaseCloseOut(
            UUID casePkId,
            UUID categoryId,
            UUID ownerId,
            String comments,
            String caseNo) {
        List<UUID> closeOutIds = db1JdbcTemplate.query(
                FIND_CASE_CLOSE_OUT_SQL,
                (rs, rowNum) -> uuid(
                        rs.getString("CaseCloseOut_PK_ID"),
                        caseNo,
                        "case closeout ID"),
                casePkId);
        if (closeOutIds.size() > 1) {
            throw failure(caseNo, "multiple target case closeouts exist.");
        }
        if (closeOutIds.size() == 1) {
            log.debug("DB1 closeout already exists for WKS case {}", caseNo);
            return;
        }

        int inserted = db1JdbcTemplate.update(
                INSERT_CASE_CLOSE_OUT_SQL,
                casePkId,
                ownerId,
                ownerId,
                categoryId,
                comments);
        if (inserted != 1) {
            throw failure(caseNo, "target case closeout insert affected an unexpected number of rows.");
        }
        log.info("Inserted DB1 closeout for WKS case {}", caseNo);
    }

    private JsonNode readContainer(Case sourceCase, String caseNo) {
        List<Attribute> attributes = sourceCase.getAttributes();
        if (attributes == null) {
            throw failure(caseNo, "case attributes are missing.");
        }

        List<Attribute> containers = attributes.stream()
                .filter(attribute -> attribute != null && "container".equals(attribute.getName()))
                .toList();
        if (containers.size() != 1 || containers.get(0).getValue() == null) {
            throw failure(caseNo, "container attributes are missing or ambiguous.");
        }

        try {
            JsonNode container = objectMapper.readTree(containers.get(0).getValue());
            if (container == null || !container.isObject()) {
                throw failure(caseNo, "container attributes are invalid.");
            }
            return container;
        } catch (JsonProcessingException e) {
            throw failure(caseNo, "container attributes are invalid.", e);
        }
    }

    private List<Integer> normalizeEventIds(List<String> eventIds, String caseNo) {
        if (eventIds == null || eventIds.isEmpty()) {
            throw failure(caseNo, "event IDs are missing.");
        }

        Map<Integer, Integer> uniqueIds = new LinkedHashMap<>();
        for (String eventId : eventIds) {
            String normalized = requiredValue(eventId, Integer.MAX_VALUE, "event ID", caseNo);
            try {
                Integer numericId = Integer.valueOf(normalized);
                uniqueIds.putIfAbsent(numericId, numericId);
            } catch (NumberFormatException e) {
                throw failure(caseNo, "event " + normalized + " is not a valid FaultHistoryClusteredId.", e);
            }
        }
        return new ArrayList<>(uniqueIds.values());
    }

    private Long requiredLongJsonValue(
            JsonNode container,
            String field,
            String label,
            String caseNo) {
        JsonNode value = container.get(field);
        if (value == null || value.isNull()
                || (value.isTextual() && value.textValue().trim().isEmpty())) {
            throw failure(caseNo, label + " is missing.");
        }

        Long parsedValue = null;
        if (value.isIntegralNumber() && value.canConvertToLong()) {
            parsedValue = value.longValue();
        } else if (value.isTextual()) {
            try {
                parsedValue = Long.valueOf(value.textValue().trim());
            } catch (NumberFormatException e) {
                throw failure(caseNo, label + " is invalid.", e);
            }
        }

        if (parsedValue == null || parsedValue <= 0) {
            throw failure(caseNo, label + " is invalid.");
        }
        return parsedValue;
    }

    private CaseCauseCategory resolveSourceCategory(JsonNode container, String caseNo) {
        JsonNode value = container.get("caseCauseCategory");
        boolean unselected = value == null
                || value.isNull()
                || (value.isTextual() && value.textValue().trim().isEmpty());
        if (!unselected) {
            Long categoryId = requiredLongJsonValue(
                    container, "caseCauseCategory", "case cause category", caseNo);
            return caseCauseCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> failure(caseNo,
                            "case cause category " + categoryId + " was not found."));
        }

        List<CaseCauseCategory> categories = caseCauseCategoryRepository.findByName(NOT_CATEGORISED);
        if (categories.isEmpty()
                || (categories.size() == 1
                        && !NOT_CATEGORISED.equals(categories.get(0).getName()))) {
            throw failure(caseNo, "Not Categorised DB2 category was not found.");
        }
        if (categories.size() > 1) {
            throw failure(caseNo, "multiple Not Categorised DB2 categories were found.");
        }
        return categories.get(0);
    }

    private String closeOutComments(JsonNode container, String caseNo) {
        JsonNode value = container.get("valueRealizationConclusion");
        if (value == null || value.isNull()) {
            return "";
        }
        if (!value.isTextual()) {
            throw failure(caseNo, "value realization conclusion is invalid.");
        }
        return value.textValue().trim();
    }

    private String requiredJsonText(
            JsonNode container,
            String field,
            int maximumLength,
            String label,
            String caseNo) {
        JsonNode value = container.get(field);
        return requiredValue(
                value == null || value.isNull() || !value.isValueNode() ? null : value.asText(),
                maximumLength,
                label,
                caseNo);
    }

    private String optionalJsonText(
            JsonNode container,
            String field,
            int maximumLength,
            String label,
            String caseNo) {
        JsonNode value = container.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isValueNode()) {
            throw failure(caseNo, label + " is invalid.");
        }
        String normalized = value.asText().trim();
        if (normalized.length() > maximumLength) {
            throw failure(caseNo, label + " exceeds " + maximumLength + " characters.");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private String requiredValue(String value, int maximumLength, String label, String caseNo) {
        if (value == null || value.trim().isEmpty()) {
            throw failure(caseNo, label + " is missing.");
        }
        String normalized = value.trim();
        if (normalized.length() > maximumLength) {
            throw failure(caseNo, label + " exceeds " + maximumLength + " characters.");
        }
        return normalized;
    }

    private UUID uuid(String value, String caseNo, String label) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException e) {
            throw failure(caseNo, label + " is invalid.", e);
        }
    }

    private CaseSynchronizationException failure(String caseNo, String message) {
        return new CaseSynchronizationException("Cannot synchronize WKS case " + caseNo + ": " + message);
    }

    private CaseSynchronizationException failure(String caseNo, String message, Throwable cause) {
        return new CaseSynchronizationException(
                "Cannot synchronize WKS case " + caseNo + ": " + message,
                cause);
    }

    private record SourceCaseData(
            String caseNo,
            String caseName,
            String description,
            String status,
            String createdBy,
            UUID ownerId,
            String caseLink,
            String content,
            Long caseCauseCategoryId,
            String categoryName,
            String closeOutComments,
            List<Integer> eventIds) {
    }

    private record TargetCase(UUID casePkId, String status, Timestamp closedDate) {
    }

    private record TargetCategory(UUID categoryPkId, String name) {
    }

    private static final class CaseSynchronizationException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private CaseSynchronizationException(String message) {
            super(message);
        }

        private CaseSynchronizationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
