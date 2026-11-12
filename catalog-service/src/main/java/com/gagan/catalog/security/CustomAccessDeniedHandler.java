package com.gagan.catalog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gagan.catalog.constant.ErrorCodeEnum;
import com.gagan.catalog.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        ErrorCodeEnum errorEnum = ErrorCodeEnum.ACCESS_DENIED;
        log.warn("Forbidden catalog request on path [{}]: code=[{}]",
                request.getRequestURI(), errorEnum.getErrorCode());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(errorEnum.getErrorCode())
                .errorMessage(errorEnum.getErrorMessage())
                .timestamp(LocalDateTime.now())
                .build();

        response.setStatus(errorEnum.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}