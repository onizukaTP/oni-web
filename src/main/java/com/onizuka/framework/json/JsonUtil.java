package com.onizuka.framework.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onizuka.framework.exception.BadRequestException;

public class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing object to JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            throw new BadRequestException("JSON body is empty or null");
        }
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new BadRequestException("Invalid JSON format: " + e.getMessage());
        }
    }

    public static ObjectMapper getMapper() {
        return mapper;
    }
}
