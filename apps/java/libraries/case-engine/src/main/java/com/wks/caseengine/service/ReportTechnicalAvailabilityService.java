package com.wks.caseengine.service;

import java.util.List;



import com.wks.caseengine.dto.TechnicalAvailabilityDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ReportTechnicalAvailabilityService {

	public AOPMessageVM getTechnicalAvailability(String siteId, String year);

	public List<TechnicalAvailabilityDTO> saveReportTechnicalAvailabilityTransaction(List<TechnicalAvailabilityDTO> technicalAvailabilityDTOs);

	public AOPMessageVM LoadTechnicalAvailability(String siteId, String aopYear);

}
