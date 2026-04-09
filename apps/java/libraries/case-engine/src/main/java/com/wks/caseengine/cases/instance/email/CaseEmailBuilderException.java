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

import java.io.Serial;

public class CaseEmailBuilderException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public CaseEmailBuilderException(String message) {
		super(message);
	}

}
