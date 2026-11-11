package com.gagan.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gagan.identity.constant.ErrorCodeEnum;
import com.gagan.identity.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        ErrorCodeEnum errorEnum = ErrorCodeEnum.INVALID_TOKEN;
        log.warn("Unauthorized request on path [{}]: code=[{}]",
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