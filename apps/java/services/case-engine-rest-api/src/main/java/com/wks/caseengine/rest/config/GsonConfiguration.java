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
package com.wks.caseengine.rest.config;

import java.util.List;

// import com.google.gson.GsonBuilder;
// import com.wks.caseengine.json.GsonBuilderFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
 import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;



@Configuration
public class GsonConfiguration {

	// @Bean
	// public GsonBuilder gsonBuilder(List<GsonBuilderCustomizer> customizers) {
	// 	return new GsonBuilderFactory().getGsonBuilder();
	// }

	@Bean
    public Gson gson() {
        return new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .create();
    }

    @Bean
    public GsonBuilder gsonBuilder() {
        return new GsonBuilder();
    }



}






