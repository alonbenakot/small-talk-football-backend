package com.smalltalk.SmallTalkFootball.system.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class ResponseHandler {

    public static boolean isValidResponse(ResponseEntity<String> response) {
        try {
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null;
        } catch (Exception e) {
            log.warn("Response is null");
            return false;
        }
    }

    public static void logResponseError(ResponseEntity<String> response, String errorMsg) {
        try {
            log.error("{} ({}): {}", errorMsg, response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            log.error(errorMsg);
        }
    }


    public static <T> Optional<T> process(
            Supplier<ResponseEntity<String>> apiCall,
            String errorMessage,
            TypeReference<T> typeReference,
            ObjectMapper objectMapper) {

        ResponseEntity<String> response = null;
        try {
            response = apiCall.get();
            log.debug(response.toString());
            if (isValidResponse(response)) {
                T result = objectMapper.readValue(response.getBody(), typeReference);
                return Optional.ofNullable(result);
            } else {
                logResponseError(response, errorMessage);
                return Optional.empty();
            }
        } catch (Exception e) {
            logResponseError(response, errorMessage + " - Exception: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static <T> Optional<T> process(
            Supplier<ResponseEntity<String>> apiCall,
            String errorMessage,
            Class<T> clazz,
            ObjectMapper objectMapper) {

        return process(apiCall, errorMessage,
                new TypeReference<T>() {
                    @Override
                    public Type getType() {
                        return clazz;
                    }
                }, objectMapper);
    }

}