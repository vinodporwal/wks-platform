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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author victor.franca
 */
@Configuration
public class RestTemplateConfig {

	// Timeout in milliseconds (10 minutes for long-running budget calculations)
	private static final int CONNECTION_TIMEOUT = 30_000;      // 30 seconds to establish connection
	private static final int READ_TIMEOUT = 600_000;           // 10 minutes to read response

	// @Bean
	// public RestTemplate getRestTemplate() {
	// 	HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
	// 	// Manual migration to `ConnectionConfig.Builder.setConnectTimeout(Timeout)` necessary; see: https://github.com/spring-projects/spring-framework/issues/35748
	// 	factory.setConnectTimeout(CONNECTION_TIMEOUT);
	// 	factory.setConnectionRequestTimeout(CONNECTION_TIMEOUT);
	// 	// Manual migration to `SocketConfig.Builder.setSoTimeout(Timeout)` necessary; see: https://docs.spring.io/spring-framework/docs/6.0.0/javadoc-api/org/springframework/http/client/HttpComponentsClientHttpRequestFactory.html#setReadTimeout(int)
	// 	factory.setReadTimeout(READ_TIMEOUT);
		
	// 	RestTemplate restTemplate = new RestTemplate();
	// 	restTemplate.setRequestFactory(factory);
	// 	return restTemplate;
	// }




// @Bean
// public RestTemplate getRestTemplate() {

//     // 1. Replace factory.setConnectTimeout()
//     //    and factory.setConnectionRequestTimeout()
//     ConnectionConfig connectionConfig = ConnectionConfig.custom()
//             .setConnectTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT))
//             .setConnectionRequestTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT))
//             .build();

//     // 2. Replace factory.setReadTimeout()
//     SocketConfig socketConfig = SocketConfig.custom()
//             .setSoTimeout(Timeout.ofMilliseconds(READ_TIMEOUT))
//             .build();

//     // 3. Wire both configs into the connection manager
//     HttpClientConnectionManager connectionManager =
//             PoolingHttpClientConnectionManagerBuilder.create()
//                     .setDefaultConnectionConfig(connectionConfig)
//                     .setDefaultSocketConfig(socketConfig)
//                     .build();

//     // 4. Build the HttpClient with the connection manager
//     HttpClient httpClient = HttpClientBuilder.create()
//             .setConnectionManager(connectionManager)
//             .build();

//     // 5. Set on the factory (no more setter methods)
//     HttpComponentsClientHttpRequestFactory factory =
//             new HttpComponentsClientHttpRequestFactory(httpClient);

//     RestTemplate restTemplate = new RestTemplate();
//     restTemplate.setRequestFactory(factory);
//     return restTemplate;
// }



    @Bean
    public RestTemplate getRestTemplate() {

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT))
                .setResponseTimeout(Timeout.ofMilliseconds(READ_TIMEOUT))
                .build();

        var httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(factory);
    }


}

