package com.wks.caseengine.rest.server;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.SiteTeamTranscationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.SiteTeamTranscationService;

@RestController
@RequestMapping("task")
public class SiteTeamTranscationController {
	
	@Autowired
	private SiteTeamTranscationService siteTeamTranscationService;
	
	@GetMapping(value = "/site-team-transaction")
	public AOPMessageVM getSiteTeamTransaction(@RequestParam String siteId, @RequestParam String year) {
		return siteTeamTranscationService.getSiteTeamTransaction(siteId, year);
	}
	
	@PostMapping(value = "/site-team-transaction")
	public AOPMessageVM saveSiteTeamTransaction(@RequestParam String year, @RequestParam String siteId, @RequestBody List<SiteTeamTranscationDTO> siteTeamTranscationDTOs) {
		return siteTeamTranscationService.saveSiteTeamTransaction(year, siteId, siteTeamTranscationDTOs);
	}

	@GetMapping(value = "/site-team-export")
	public ResponseEntity<byte[]> exportSiteTeam(
			@RequestParam("siteId") String siteId,
			@RequestParam("year") String year) {
		try {
			byte[] excelBytes = siteTeamTranscationService.exportSiteTeam(year, siteId, false, null);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.builder("attachment")
					.filename("site_team.xlsx")
					.build());
			headers.setContentLength(excelBytes.length);

			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/site-team-import", consumes = "multipart/form-data")
	public AOPMessageVM importSiteTeam(
			@RequestParam("siteId") String siteId,
			@RequestParam("year") String year,
			@RequestParam("file") MultipartFile file) {
		return siteTeamTranscationService.importSiteTeam(year, UUID.fromString(siteId), file);
	}

}
