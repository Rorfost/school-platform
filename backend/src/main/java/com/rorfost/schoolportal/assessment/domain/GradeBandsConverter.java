package com.rorfost.schoolportal.assessment.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Converter
public class GradeBandsConverter implements AttributeConverter<List<GradeBand>, String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<GradeBand>> TYPE_REF = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<GradeBand> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize grade bands to JSON", e);
        }
    }

    @Override
    public List<GradeBand> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, TYPE_REF);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
