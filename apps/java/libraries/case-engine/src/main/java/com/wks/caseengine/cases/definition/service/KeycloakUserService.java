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
package com.wks.caseengine.cases.definition.service;

import java.util.Map;

public interface KeycloakUserService {

	Map<String, Object> searchUsers(String searchString) throws Exception;

	Map<String, Object> getAllGroups() throws Exception;

	Map<String, Object> getRealmRoles() throws Exception;

	Map<String, Object> getUsers() throws Exception;

}
