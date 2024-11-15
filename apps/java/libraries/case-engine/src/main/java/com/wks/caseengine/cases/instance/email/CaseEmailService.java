/*
 * WKS Platform - Open-Source Project
 * 
 * This file is part of the WKS Platform, an open-source project developed by WKS Power.
 * 
 * WKS Platform is licensed under the MIT License.
 * 
 * © 2021 WKS Power. All rights reserved.
 * 
 * For licensing information, see the LICENSE file in the root directory of the project.
 */
package com.wks.caseengine.cases.instance.email;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

public interface CaseEmailService {

	List<CaseEmail> find(final Optional<String> businessKey);

	void start(final CaseEmail caseEmail);

	CaseEmail save(final CaseEmail caseEmail);

	void markAsSent(final String id, final Date sentDateTime);

	void patch(final String id, final CaseEmail mergePatch);

}
