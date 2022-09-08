package com.wks.caseengine.rest.app;

import java.util.List;

import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.gson.GsonBuilder;
import com.wks.caseengine.cases.definition.event.CaseEvent;
import com.wks.caseengine.cases.definition.event.CaseEventDeserializer;
import com.wks.caseengine.cases.definition.event.CaseEventSerializer;

@Configuration
public class GsonConfiguration {

	@Bean
	public GsonBuilder gsonBuilder(List<GsonBuilderCustomizer> customizers) {

		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(CaseEvent.class, new CaseEventDeserializer());
		builder.registerTypeAdapter(CaseEvent.class, new CaseEventSerializer<CaseEvent>());

		return builder;
	}

}
