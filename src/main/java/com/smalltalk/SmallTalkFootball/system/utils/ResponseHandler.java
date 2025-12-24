package com.smalltalk.SmallTalkFootball.system.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

    public static <T> Stream<T> process(
            Supplier<ResponseEntity<String>> apiCall,
            String errorMessage,
            TypeReference<List<T>> typeReference,
            ObjectMapper objectMapper) {

        ResponseEntity<String> response = null;
        try {
            response = apiCall.get();

            if (isValidResponse(response)) {
                List<T> items = objectMapper.readValue(response.getBody(), typeReference);
                return items.stream();
            } else {
                logResponseError(response, errorMessage);
                return Stream.empty();
            }
        } catch (Exception e) {
            logResponseError(response, errorMessage + " - Exception: " + e.getMessage());
            return Stream.empty();
        }
    }

}
