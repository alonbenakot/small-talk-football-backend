package com.smalltalk.SmallTalkFootball.system.utils;

import org.springframework.http.ResponseEntity;

public class ResponseHandler {

    public static boolean isValidResponse(ResponseEntity<String> response) {
        try {
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null;
        } catch (Exception e) {
            System.err.println("Response is null");
            return false;
        }
    }

    public static void logResponseError(ResponseEntity<String> response, String errorMsg) {
        try {
            System.err.println(errorMsg + " (" + response.getStatusCode() + "): " + response.getBody());
        } catch (Exception e) {
            System.err.println(errorMsg);
        }
    }

}
