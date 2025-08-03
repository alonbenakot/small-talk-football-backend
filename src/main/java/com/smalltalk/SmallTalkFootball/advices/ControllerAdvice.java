package com.smalltalk.SmallTalkFootball.advices;

import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControllerAdvice {

    @ExceptionHandler(value = SmallTalkException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public SmallTalkResponse exceptionHandler(Exception e) {
        return new SmallTalkResponse<>(e.getMessage(), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(value = NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public SmallTalkResponse noResourceFoundHandler(Exception e) {
        return new SmallTalkResponse<>(e.getMessage(), HttpStatus.NOT_FOUND.value());
    }
}
