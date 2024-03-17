package com.smalltalk.SmallTalkFootball.advices;

import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControllerAdvice {

    @ExceptionHandler(value = Exception.class)
    @ResponseStatus(HttpStatus.NON_AUTHORITATIVE_INFORMATION)
    public SmallTalkResponse exceptionHandler(Exception e) {
        return new SmallTalkResponse<>(e.getMessage(), HttpStatus.NON_AUTHORITATIVE_INFORMATION.value());
    }
}
