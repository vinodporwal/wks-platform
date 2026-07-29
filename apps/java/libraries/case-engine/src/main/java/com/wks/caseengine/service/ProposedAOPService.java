package com.wks.caseengine.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.ProposedAOPDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ProposedAOPService {

    public AOPMessageVM getProposedAOP(UUID plantId, String aopYear, UUID gradeId);

    public AOPMessageVM calculateProposedAOP(UUID plantId, String aopYear);

    public AOPMessageVM saveProposedAOP(List<ProposedAOPDTO> dtoList);

    public byte[] createProposedAOPExcel(UUID plantId, String aopYear, boolean isAfterSave, List<ProposedAOPDTO> dtoList);

    public AOPMessageVM importProposedAOPExcel(MultipartFile file);
}
