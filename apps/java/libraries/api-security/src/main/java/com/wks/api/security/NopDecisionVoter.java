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
package com.wks.api.security;

import java.util.Collection;

// import org.springframework.security.access.AccessDecisionVoter;
// import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.jspecify.annotations.Nullable;

// public final class NopDecisionVoter implements AccessDecisionVoter<Object> {

// 	@Override
// 	public boolean supports(ConfigAttribute attribute) {
// 		return false;
// 	}

// 	@Override
// 	public boolean supports(Class<?> clazz) {
// 		return false;
// 	}

// 	@Override
// 	public int vote(Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {
// 		return ACCESS_GRANTED;
// 	}

// }

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;

import java.util.function.Supplier;

public final class NopDecisionVoter implements AuthorizationManager<Object> {

   

    @Override
    public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> arg0, Object arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'authorize'");
    }

   
}
