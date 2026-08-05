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
package com.wks.bpm.engine.camunda.client.importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Deploys BPMN/DMN definitions to a standalone Camunda 7 engine via REST.
 *
 * <p>Ships inside {@code c7-client} so production does not need demo-data-loader.
 * Enable with {@code camunda7.data.import.enabled=true} on any Spring Boot app
 * that component-scans {@link com.wks.bpm.engine.camunda.client.config.Camunda7ClientScan}
 * (e.g. case-engine-rest-api).</p>
 *
 * <p>Sources, in order:</p>
 * <ol>
 *   <li>{@code camunda7.data.import.folder} — filesystem directory, when set and present</li>
 *   <li>classpath {@code camunda7/*.{bpmn,dmn}} — bundled in this jar</li>
 * </ol>
 */
@Component
@ConditionalOnProperty(name = "camunda7.data.import.enabled", havingValue = "true")
@Order(3)
@Slf4j
public class Camunda7DataImportCommandRunner implements CommandLineRunner {

	private static final String CLASSPATH_PATTERN = "classpath*:camunda7/*";

	@Value("${camunda7.data.import.folder:}")
	private String importDir;

	@Value("${camunda7.data.import.url}")
	private String baseUrl;

	@Value("${camunda7.data.import.tenant}")
	private String tenantId;

	@Override
	public void run(String... args) throws IOException {
		log.info("Starting upload model to camunda....");

		createTenant();

		importData();

		log.info("Finish upload model to camunda");
	}

	private void createTenant() {
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		String body = String.format("{\"id\":\"%s\", \"name\":\"Tenant Platform\"}", tenantId);

		HttpEntity<String> entity = new HttpEntity<>(body, headers);

		try {
			restTemplate.getForEntity(String.format("%s/tenant/%s", baseUrl, tenantId), String.class);
		} catch (RestClientException e) {
			ResponseEntity<String> responseEntity = restTemplate.exchange(String.format("%s/tenant/create", baseUrl),
					HttpMethod.POST, entity, String.class);

			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				log.info("Camunda created tenant {}", tenantId);
			} else if (responseEntity.getStatusCode().is5xxServerError()) {
				log.error("Error to create tenant {}. Error: {}", tenantId, responseEntity.toString());
			}
		}
	}

	private void importData() throws IOException {
		List<Resource> resources = resolveResources();
		if (resources.isEmpty()) {
			log.warn("No BPMN/DMN resources found (folder='{}', classpath='{}')", importDir, CLASSPATH_PATTERN);
			return;
		}

		RestTemplate restTemplate = new RestTemplate();
		for (Resource resource : resources) {
			deploy(restTemplate, resource);
		}
	}

	private List<Resource> resolveResources() throws IOException {
		if (StringUtils.hasText(importDir)) {
			Path dir = Paths.get(importDir);
			if (Files.isDirectory(dir)) {
				try (Stream<Path> stream = Files.list(dir)) {
					List<Resource> fromFolder = stream
							.filter(file -> !Files.isDirectory(file))
							.filter(this::isBpmnOrDmnFile)
							.map(Path::toAbsolutePath)
							.map(p -> (Resource) new FileSystemResource(p.toFile()))
							.collect(Collectors.toList());
					if (!fromFolder.isEmpty()) {
						log.info("Importing {} Camunda model(s) from folder {}", fromFolder.size(), importDir);
						return fromFolder;
					}
				}
			} else {
				log.warn("camunda7.data.import.folder does not exist: {}", importDir);
			}
		}

		Resource[] classpath = new PathMatchingResourcePatternResolver().getResources(CLASSPATH_PATTERN);
		List<Resource> fromClasspath = Arrays.stream(classpath)
				.filter(Resource::isReadable)
				.filter(r -> {
					String name = r.getFilename();
					return name != null && (name.endsWith(".bpmn") || name.endsWith(".dmn"));
				})
				.collect(Collectors.toCollection(ArrayList::new));
		log.info("Importing {} Camunda model(s) from classpath {}", fromClasspath.size(), CLASSPATH_PATTERN);
		return fromClasspath;
	}

	private void deploy(RestTemplate restTemplate, Resource resource) {
		String name = resource.getFilename() != null ? resource.getFilename() : resource.getDescription();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("upload", resource).filename(name);
		multipartBodyBuilder.part("tenant-id", tenantId);
		multipartBodyBuilder.part("deployment-name", name);
		multipartBodyBuilder.part("enable-duplicate-filtering", "true");

		MultiValueMap<String, HttpEntity<?>> multipartBody = multipartBodyBuilder.build();
		HttpEntity<MultiValueMap<String, HttpEntity<?>>> httpEntity = new HttpEntity<>(multipartBody, headers);

		String action = String.format("%s/deployment/create", baseUrl);
		try {
			ResponseEntity<String> responseEntity = restTemplate.postForEntity(action, httpEntity, String.class);
			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				log.info("Camunda file imported {}", name);
			} else {
				log.error("Failed to import {}: {}", name, responseEntity.getStatusCode());
			}
		} catch (RestClientException ex) {
			log.error("Failed to import {}: {}", name, ex.getMessage());
		}
	}

	private boolean isBpmnOrDmnFile(Path file) {
		String fileName = file.toFile().getName();
		return fileName.endsWith(".bpmn") || fileName.endsWith(".dmn");
	}

}
