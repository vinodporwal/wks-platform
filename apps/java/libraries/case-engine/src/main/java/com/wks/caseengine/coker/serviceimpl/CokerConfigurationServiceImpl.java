package com.wks.caseengine.coker.serviceimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.coker.dto.CokerConfigurationDto;
import com.wks.caseengine.coker.service.CokerConfigurationService;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class CokerConfigurationServiceImpl implements CokerConfigurationService {

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AOPMessageVM getConfigurationData(String year, UUID plantFKId, String type, String version) {
        try {
            String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
            Plants plant = plantsRepository.findById((plantFKId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

            List<Object[]> obj = new ArrayList<>();
            obj = findManualEntryByYearAndPlantFkId(year, plantFKId);

            // NormParameter_FK_Id |DisplayName |Jan |Feb |Mar |Apr |May |Jun |Jul |Aug |Sep
            // |Oct |Nov |Dec |Remarks |UOM |NormParameterTypeDisplayName |Type
            // B3BCCFFB-106D-4136-A690-E9CFA3688A1C|Pigging (P)/Non-Pigging (NP) |P-4 |P-4
            // |P-4 |P-4 |P-4 |P-4 |P-4 |P-4 |P-4 |P-4 |P-4 |P-4 |Test |[NULL] |Manual Entry
            // |[NULL]

            // Debug logging
            for (Object[] row : obj) {
                System.out.println("=== SQL Result Row ===");
                System.out.println("Row length: " + row.length);
                for (int i = 0; i < row.length; i++) {
                    System.out.println("[" + i + "]: " + (row[i] != null ? row[i].toString() : "NULL"));
                }
            }

            List<CokerConfigurationDto> configurationDTOList = new ArrayList<>();
            for (Object[] row : obj) {
                CokerConfigurationDto configurationDTO = mapRowToDto(row);
                configurationDTOList.add(configurationDTO);
            }

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            aopMessageVM.setCode(200);
            aopMessageVM.setData(configurationDTOList);
            aopMessageVM.setMessage("Data fetched successfully");
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to fetch data", ex);
        }
    }

    public List<Object[]> findManualEntryByYearAndPlantFkId(String year, UUID plantFKId) {

        try {

            String sql = "SELECT " +
                    "    NP.Id AS NormParameter_FK_Id, " +
                    "    NP.DisplayName, " +

                    "    MAX(CASE WHEN NAT.AOPMonth = 1 THEN NAT.AttributeValue END) AS Jan, " +
                    "    MAX(CASE WHEN NAT.AOPMonth = 2 THEN NAT.AttributeValue END) AS Feb, " +
                    "    MAX(CASE WHEN NAT.AOPMonth = 3 THEN NAT.AttributeValue END) AS Mar, " +
                    "    MAX(CASE WHEN NAT.AOPMonth = 4 THEN NAT.AttributeValue END) AS Apr, " +
                    "    MAX(CASE WHEN NAT.AOPMonth = 5 THEN NAT.AttributeValue END) AS May, " +
                    "    MAX(CASE WHEN NAT.AOPMonth = 6 THEN NAT.AttributeValue END) AS Jun, " +
                    "    MAX(CASE WHEN NAT.AOPMonth = 7 THEN NAT.AttributeValue END) AS Jul, " +
                    "    MAX(CASE WHEN NAT.AOPMonth = 8 THEN NAT.AttributeValue END) AS Aug, " +
                    "    MAX(CASE WHEN NAT.AOPMonth = 9 THEN NAT.AttributeValue END) AS Sep, " +
                    "    MAX(CASE WHEN NAT.AOPMonth = 10 THEN NAT.AttributeValue END) AS Oct, " +
                    "    MAX(CASE WHEN NAT.AOPMonth = 11 THEN NAT.AttributeValue END) AS Nov, " +
                    "    MAX(CASE WHEN NAT.AOPMonth = 12 THEN NAT.AttributeValue END) AS Dec, " +

                    "    MAX(NAT.Remarks) AS Remarks, " +
                    "    MAX(NAT.AuditYear) AS AuditYear, " +
                    "    NP.UOM, " +
                    "    MAX(NPT.DisplayName) AS NormParameterTypeDisplayName, " +
                    "    NP.Type " +

                    "FROM NormParameters NP " +

                    "JOIN NormParameterType NPT " +
                    "    ON NP.NormParameterType_FK_Id = NPT.Id " +

                    "LEFT JOIN NormAttributeTransactions NAT " +
                    "    ON NAT.NormParameter_FK_Id = NP.Id " +
                    "    AND NAT.AuditYear = :year " +

                    "WHERE NP.Plant_FK_Id = :plantFKId " +
                    "    AND NPT.Name = 'Manual Entry' " +

                    "GROUP BY " +
                    "    NP.Id, " +
                    "    NP.DisplayName, " +
                    "    NP.DisplayOrder, " +
                    "    NP.UOM, " +
                    "    NP.Type " +

                    "ORDER BY NP.DisplayOrder";

            Query query = entityManager.createNativeQuery(sql);

            query.setParameter("year", year);
            query.setParameter("plantFKId", plantFKId);

            return query.getResultList();

        } catch (Exception e) {
            throw new RuntimeException("Error fetching Manual Entry data", e);
        }
    }

    /**
     * Map SQL result row to CokerConfigurationDto
     * SQL Column Order (19 columns - indices 0-18):
     * 0: NormParameter_FK_Id (UUID)
     * 1: DisplayName (String)
     * 2-13: Jan-Dec (String values)
     * 14: Remarks (String)
     * 15: AuditYear (String)
     * 16: UOM (String)
     * 17: NormParameterTypeDisplayName (String)
     * 18: Type (String)
     */
    private CokerConfigurationDto mapRowToDto(Object[] row) {
        CokerConfigurationDto dto = new CokerConfigurationDto();

        // Column 0: NormParameter_FK_Id
        dto.setNormParameterFKId(row[0] != null ? row[0].toString() : "");

        // Column 1: DisplayName (maps to productName)
        dto.setProductName(row[1] != null ? row[1].toString() : "");

        // Columns 2-13: Month values (Jan-Dec)
        dto.setJan(row[2] != null ? row[2].toString() : "");
        dto.setFeb(row[3] != null ? row[3].toString() : "");
        dto.setMar(row[4] != null ? row[4].toString() : "");
        dto.setApr(row[5] != null ? row[5].toString() : "");
        dto.setMay(row[6] != null ? row[6].toString() : "");
        dto.setJun(row[7] != null ? row[7].toString() : "");
        dto.setJul(row[8] != null ? row[8].toString() : "");
        dto.setAug(row[9] != null ? row[9].toString() : "");
        dto.setSep(row[10] != null ? row[10].toString() : "");
        dto.setOct(row[11] != null ? row[11].toString() : "");
        dto.setNov(row[12] != null ? row[12].toString() : "");
        dto.setDec(row[13] != null ? row[13].toString() : "");

        // Column 14: Remarks
        dto.setRemarks(row[14] != null ? row[14].toString() : "");

        // Column 15: AuditYear
        dto.setAuditYear(row[15] != null ? row[15].toString() : "");

        // Column 16: UOM
        dto.setUOM(row[16] != null ? row[16].toString() : "");

        // Column 17: NormParameterTypeDisplayName
        dto.setNormParameterTypeDisplayName(row[17] != null ? row[17].toString() : "");

        // Column 18: Type
        dto.setType(row[18] != null ? row[18].toString() : "");

        return dto;
    }
}
