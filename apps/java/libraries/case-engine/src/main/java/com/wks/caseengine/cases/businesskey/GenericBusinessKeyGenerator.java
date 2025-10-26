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

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wks.caseengine.cases.instance.CaseInstance;
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

	@Override
	 public String generate(String caseDefinitionKey) {
		
		return calculateBusinessKey(caseDefinitionKey);
	 }

	private String calculateBusinessKey(String... args) {
		String latestKey;
		try {
			if(args.length == 0) {
				latestKey = caseInstanceRepository.findLatestByCreatedAt().getBusinessKey();
			} else {
				latestKey = caseInstanceRepository.findLatestByCreatedAt(args).getBusinessKey();
			}
			
		} catch (DatabaseRecordNotFoundException e) {
			 latestKey = String.valueOf(PREFIX + ThreadLocalRandom.current().nextInt(0, 100000 + 1));
		}
    
    
    int nextKey = Integer.parseInt(latestKey) + 1;

	// safeguard against duplicate businessKey in caseInstance
	List<CaseInstance> allCases = caseInstanceRepository.find();

	boolean exists = true;
	while (exists) {
		String keyToCheck = String.valueOf(nextKey);
	    exists = allCases.stream().anyMatch(ci -> ci.getBusinessKey().equals(keyToCheck));
		if (exists) {
			nextKey++;
		}
	}
		
	
    
    return String.valueOf(nextKey);
	//	return String.valueOf(PREFIX + ThreadLocalRandom.current().nextInt(0, 100000 + 1));
	}

}
