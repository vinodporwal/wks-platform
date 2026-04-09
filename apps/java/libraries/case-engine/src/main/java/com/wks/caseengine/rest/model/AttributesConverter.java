package com.wks.caseengine.rest.model;

import java.util.List;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;

@Converter(autoApply = true)
public class AttributesConverter implements AttributeConverter<List<Attribute>, String> {

    private final ObjectMapper objectMapper = new JsonMapper();

    @Override
    public String convertToDatabaseColumn(List<Attribute> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to convert attributes to JSON", e);
        }
    }

    @Override
    public List<Attribute> convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, objectMapper.getTypeFactory().constructCollectionType(List.class, Attribute.class));
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to convert JSON to attributes", e);
        }
    }
}
