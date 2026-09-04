package com.wks.caseengine.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.GradeSelectionDTO;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.utility.Utility;
import com.wks.caseengine.message.vm.AOPMessageVM;

@Service
public class GradeSelectionServiceImpl implements GradeSelectionService {
   
 @Autowired
 private JdbcTemplate jdbcTemplate;

 @Autowired
 private NormAttributeTransactionsRepository normAttributeTransactionsRepository;

 @Override
 public AOPMessageVM getGradeSelection(String plantFKId, String year) {

    String storedProcedure = "usp_GetPlantProductGrades";
       
    List<GradeSelectionDTO> gradeSelectionDTOs = fetchGradeSelectionFromSp(plantFKId, year, storedProcedure);

    AOPMessageVM aopMessageVM = new AOPMessageVM();
    aopMessageVM.setCode(200);
    aopMessageVM.setMessage("Grade selection fetched successfully");
    aopMessageVM.setData(gradeSelectionDTOs);
    return aopMessageVM;

    }

    public List<GradeSelectionDTO> fetchGradeSelectionFromSp(String plantFKId, String year, String procedureName) {
        String sql = "EXEC " + procedureName + " @plantFKId = ?, @year = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            GradeSelectionDTO.builder()
                .normParameterId(rs.getString("NormParameterId"))
                .gradeId(rs.getString("GradeId"))
                .materialName(rs.getString("MaterialName"))
                .isSelected(rs.getObject("IsSelected") != null ? rs.getBoolean("IsSelected") : false)
                .remarks(rs.getString("Remarks"))
                .build(), plantFKId, year);
    }

    @Override
    @Transactional
    public AOPMessageVM saveGradeSelection(List<GradeSelectionDTO> gradeSelectionDTOs, String year) {

        if(gradeSelectionDTOs == null || gradeSelectionDTOs.isEmpty()) {
           throw new RuntimeException("Grade selection data is empty");
        }

        if(year == null || year.isEmpty()) {
            throw new RuntimeException("Year is required");
        }

        List<GradeSelectionDTO> selectedGrades = gradeSelectionDTOs.stream()
            .filter(GradeSelectionDTO::isSelected)
            .toList();

        String selectedGradeIds = selectedGrades.stream()
            .map(GradeSelectionDTO::getGradeId)
            .filter(id -> id != null && !id.isEmpty())
            .distinct()
            .reduce((id1, id2) -> id1 + "," + id2)
            .orElse("");

      UUID normParameterFKId =  gradeSelectionDTOs.get(0).getNormParameterId() != null
            ? UUID.fromString(gradeSelectionDTOs.get(0).getNormParameterId())
            : null;

     if(normParameterFKId == null) {
        throw new RuntimeException("Norm parameter ID is required");
     }

     String remark =  gradeSelectionDTOs.get(0).getRemarks() != null
     ? gradeSelectionDTOs.get(0).getRemarks()
     : "";

       
    Optional<NormAttributeTransactions> existingRecord = normAttributeTransactionsRepository
			.findByNormParameterFKIdAndAOPMonthAndAuditYear(normParameterFKId, 4, year);

	NormAttributeTransactions normAttributeTransactions;

	if (existingRecord.isPresent()) {
		normAttributeTransactions = existingRecord.get();
		normAttributeTransactions.setModifiedOn(new Date());
	} else {

		normAttributeTransactions = new NormAttributeTransactions();
		normAttributeTransactions.setCreatedOn(new Date());
		normAttributeTransactions.setUserName(Utility.getUserName());
		normAttributeTransactions.setNormParameterFKId(normParameterFKId);
		normAttributeTransactions.setAopMonth(4);
		normAttributeTransactions.setAuditYear(year);
	}

	normAttributeTransactions
			.setAttributeValue(selectedGradeIds != null ? selectedGradeIds: "");
	normAttributeTransactions.setRemarks(remark);
	normAttributeTransactions.setUserName(Utility.getUserName());
	normAttributeTransactionsRepository.save(normAttributeTransactions);
   

    AOPMessageVM aopMessageVM = new AOPMessageVM();
    aopMessageVM.setCode(200);
    aopMessageVM.setMessage("Grade selection saved successfully");
    return aopMessageVM;
       
    }
}
