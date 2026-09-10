package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.GradeSelectionDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface GradeSelectionService {
    
    public AOPMessageVM getGradeSelection( String plantFKId, String year);

    public AOPMessageVM saveGradeSelection(List<GradeSelectionDTO> gradeSelectionDTOs, String year);
}
