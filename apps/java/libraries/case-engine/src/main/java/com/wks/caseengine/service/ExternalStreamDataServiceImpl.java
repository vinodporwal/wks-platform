package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.ExternalStreamDataDTO;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class ExternalStreamDataServiceImpl implements ExternalStreamDataService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private NormAttributeTransactionsRepository normAttributeTransactionsRepository;

    @Override
    public AOPMessageVM getExternalStreamData(String plantId, String siteId, String verticalId, String aopYear) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            String sql = "EXEC CRACKER_VMD_GetExternalStreamData "
                    + "@plantId = :plantId, @siteId = :siteId, @verticalId = :verticalId, @AopYear = :aopYear";

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("plantId", plantId);
            query.setParameter("siteId", siteId);
            query.setParameter("verticalId", verticalId);
            query.setParameter("aopYear", aopYear);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<ExternalStreamDataDTO> list = new ArrayList<>();
            for (Object[] row : results) {
                ExternalStreamDataDTO dto = new ExternalStreamDataDTO();
                dto.setVerticalId(row[0] != null ? row[0].toString() : null);
                dto.setPlantId(row[1] != null ? row[1].toString() : null);
                dto.setNormParameterId(row[2] != null ? row[2].toString() : null);
                dto.setParticulars(row[3] != null ? row[3].toString() : null);
                dto.setNormParameterTypeDisplayName(row[4] != null ? row[4].toString() : null);
                dto.setNormParameterTypeFkId(row[5] != null ? row[5].toString() : null);
                dto.setType(row[6] != null ? row[6].toString() : null);
                dto.setUom(row[7] != null ? row[7].toString() : null);
                dto.setAuditYear(row[8] != null ? row[8].toString() : null);
                dto.setRemarks(row[9] != null ? row[9].toString() : null);

                dto.setJan(row[10] instanceof Number ? ((Number) row[10]).doubleValue() : null);
                dto.setFeb(row[11] instanceof Number ? ((Number) row[11]).doubleValue() : null);
                dto.setMar(row[12] instanceof Number ? ((Number) row[12]).doubleValue() : null);
                dto.setApr(row[13] instanceof Number ? ((Number) row[13]).doubleValue() : null);
                dto.setMay(row[14] instanceof Number ? ((Number) row[14]).doubleValue() : null);
                dto.setJun(row[15] instanceof Number ? ((Number) row[15]).doubleValue() : null);
                dto.setJul(row[16] instanceof Number ? ((Number) row[16]).doubleValue() : null);
                dto.setAug(row[17] instanceof Number ? ((Number) row[17]).doubleValue() : null);
                dto.setSep(row[18] instanceof Number ? ((Number) row[18]).doubleValue() : null);
                dto.setOct(row[19] instanceof Number ? ((Number) row[19]).doubleValue() : null);
                dto.setNov(row[20] instanceof Number ? ((Number) row[20]).doubleValue() : null);
                dto.setDec(row[21] instanceof Number ? ((Number) row[21]).doubleValue() : null);

                dto.setIsEditable(row[22] instanceof Number ? ((Number) row[22]).intValue() : null);
                dto.setNormParameterDisplayOrder(row[23] instanceof Number ? ((Number) row[23]).intValue() : null);
                dto.setName(row[24] != null ? row[24].toString() : null);
                dto.setRno(row[25] instanceof Number ? ((Number) row[25]).intValue() : null);

                list.add(dto);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("externalStreamDataList", list);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid input", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch external stream data", ex);
        }
    }

    @Override
    public AOPMessageVM saveExternalStreamData(String year, List<ExternalStreamDataDTO> dtoList) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        if (dtoList == null || dtoList.isEmpty()) {
            aopMessageVM.setCode(400);
            aopMessageVM.setMessage("Request body cannot be empty");
            return aopMessageVM;
        }

        try {
            for (ExternalStreamDataDTO dto : dtoList) {
                if (dto.getNormParameterId() == null || dto.getNormParameterId().isBlank()) {
                    continue;
                }
                UUID normParameterId = UUID.fromString(dto.getNormParameterId());

                saveOrUpdateMonthValue(normParameterId, 1, year, dto.getJan(), dto.getRemarks());
                saveOrUpdateMonthValue(normParameterId, 2, year, dto.getFeb(), dto.getRemarks());
                saveOrUpdateMonthValue(normParameterId, 3, year, dto.getMar(), dto.getRemarks());
                saveOrUpdateMonthValue(normParameterId, 4, year, dto.getApr(), dto.getRemarks());
                saveOrUpdateMonthValue(normParameterId, 5, year, dto.getMay(), dto.getRemarks());
                saveOrUpdateMonthValue(normParameterId, 6, year, dto.getJun(), dto.getRemarks());
                saveOrUpdateMonthValue(normParameterId, 7, year, dto.getJul(), dto.getRemarks());
                saveOrUpdateMonthValue(normParameterId, 8, year, dto.getAug(), dto.getRemarks());
                saveOrUpdateMonthValue(normParameterId, 9, year, dto.getSep(), dto.getRemarks());
                saveOrUpdateMonthValue(normParameterId, 10, year, dto.getOct(), dto.getRemarks());
                saveOrUpdateMonthValue(normParameterId, 11, year, dto.getNov(), dto.getRemarks());
                saveOrUpdateMonthValue(normParameterId, 12, year, dto.getDec(), dto.getRemarks());
            }

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data saved successfully");
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid input", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save external stream data", ex);
        }
    }

    private void saveOrUpdateMonthValue(UUID normParameterId,
                                        int month,
                                        String year,
                                        Double value,
                                        String remarks) {
        if (value == null) {
            return;
        }

        Optional<NormAttributeTransactions> existingOpt =
                normAttributeTransactionsRepository
                        .findByNormParameterFKIdAndAOPMonthAndAuditYear(normParameterId, month, year);

        Date now = new Date();
        String userName = Utility.getUserName();
        String valueStr = value.toString();

        if (existingOpt.isPresent()) {
            NormAttributeTransactions nat = existingOpt.get();
            nat.setAttributeValue(valueStr);
            nat.setRemarks(remarks);
            nat.setModifiedOn(now);
            nat.setUserName(userName);
            normAttributeTransactionsRepository.save(nat);
        } else {
            NormAttributeTransactions nat = new NormAttributeTransactions();
            nat.setNormParameterFKId(normParameterId);
            nat.setAopMonth(month);
            nat.setAuditYear(year);
            nat.setAttributeValue(valueStr);
            nat.setRemarks(remarks);
            nat.setCreatedOn(now);
            nat.setUserName(userName);
            normAttributeTransactionsRepository.save(nat);
        }
    }
}

