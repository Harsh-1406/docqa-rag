package com.harshshah.docqa.common.api;

import com.harshshah.docqa.common.EmbeddingException;
import com.harshshah.docqa.common.InvalidRequestException;
import com.harshshah.docqa.common.LlmException;
import com.harshshah.docqa.common.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail invalid(InvalidRequestException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail typeMismatch(MethodArgumentTypeMismatchException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + e.getName() + "'.");
    }

    @ExceptionHandler(EmbeddingException.class)
    ProblemDetail embedding(EmbeddingException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(LlmException.class)
    ProblemDetail llm(LlmException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(RestClientException.class)
    ProblemDetail upstream(RestClientException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "Could not read the response from the model provider.");
    }
}