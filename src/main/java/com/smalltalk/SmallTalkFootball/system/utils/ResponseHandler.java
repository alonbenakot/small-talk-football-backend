package com.smalltalk.SmallTalkFootball.system.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

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
            log.error(errorMsg + " (" + response.getStatusCode() + "): " + response.getBody());
        } catch (Exception e) {
            log.error(errorMsg);
        }
    }

}
