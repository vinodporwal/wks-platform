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
package com.wks.caseengine.cases.businesskey;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wks.caseengine.cases.instance.repository.CaseInstanceRepository;
import com.wks.caseengine.repository.DatabaseRecordNotFoundException;

@Component
public class GenericBusinessKeyGenerator implements BusinessKeyGenerator {

	@Autowired
	private CaseInstanceRepository caseInstanceRepository;

	public static final String PREFIX = "";

	@Override
	public String generate() {
		return calculateBusinessKey();
	}

	private String calculateBusinessKey() {
		String latestKey;
		try {
			latestKey = caseInstanceRepository.findLatestByCreatedAt().getBusinessKey();
			System.out.println("GenericBusinessKeyGenerator *************latestKey************: " + latestKey);
		} catch (DatabaseRecordNotFoundException e) {
			 latestKey = String.valueOf(PREFIX + ThreadLocalRandom.current().nextInt(0, 100000 + 1));
		}
    
    
    int nextKey = Integer.parseInt(latestKey) + 1;
    
    return String.valueOf(nextKey);
	//	return String.valueOf(PREFIX + ThreadLocalRandom.current().nextInt(0, 100000 + 1));
	}

}
