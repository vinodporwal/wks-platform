package com.wks.caseengine.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.SiteTeamTranscationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface SiteTeamTranscationService {
	
	public AOPMessageVM getSiteTeamTransaction(String siteId, String year);
	public AOPMessageVM saveSiteTeamTransaction(String year, String siteId, List<SiteTeamTranscationDTO> siteTeamTranscationDTOs);
	public byte[] exportSiteTeam(String year, String siteId, boolean isAfterSave, List<SiteTeamTranscationDTO> dtoList);
	public AOPMessageVM importSiteTeam(String year, UUID siteId, MultipartFile file);
}
