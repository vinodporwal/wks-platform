package com.wks.caseengine.service;

import java.util.List;
import java.util.UUID;

import com.wks.caseengine.dto.ProposedAOPDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ProposedAOPService {

    public AOPMessageVM getProposedAOP(UUID plantId, String aopYear, UUID gradeId);

    public AOPMessageVM calculateProposedAOP(UUID plantId, String aopYear);

    public AOPMessageVM saveProposedAOP(List<ProposedAOPDTO> dtoList);
}
