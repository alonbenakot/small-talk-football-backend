package com.smalltalk.SmallTalkFootball.testsupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smalltalk.SmallTalkFootball.config.ObjectMapperConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Reads JSON test fixtures using the very same ObjectMapper the application uses for
 * apifootball.com responses, so that a change to the production naming strategy shows up
 * here as a failing test rather than as silently divergent test expectations.
 */
public final class JsonFixtures {

    private static final ObjectMapper API_CLIENT_MAPPER = new ObjectMapperConfig().apiClientObjectMapper();

    private JsonFixtures() {
    }

    public static ObjectMapper apiClientMapper() {
        return API_CLIENT_MAPPER;
    }

    public static <T> T parse(String json, Class<T> type) {
        try {
            return API_CLIENT_MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse JSON: " + json, e);
        }
    }

    public static <T> T parse(String json, TypeReference<T> type) {
        try {
            return API_CLIENT_MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse JSON: " + json, e);
        }
    }

    public static <T> T load(String resourcePath, Class<T> type) {
        return parse(read(resourcePath), type);
    }

    public static <T> T load(String resourcePath, TypeReference<T> type) {
        return parse(read(resourcePath), type);
    }

    /**
     * Returns a fixture's raw text, for use as a canned HTTP response body.
     */
    public static String read(String resourcePath) {
        try (InputStream in = JsonFixtures.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing test fixture on the classpath: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read test fixture: " + resourcePath, e);
        }
    }
}
