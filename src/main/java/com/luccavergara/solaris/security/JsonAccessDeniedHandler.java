package com.luccavergara.solaris.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luccavergara.solaris.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        String reason = OrganizationSecurity.consumeLastDenialReason();
        String message = reason != null && !reason.isBlank()
                ? reason
                : accessDeniedException.getMessage() != null && !accessDeniedException.getMessage().isBlank()
                ? accessDeniedException.getMessage()
                : "You do not have permission to perform this action";

        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .status(HttpServletResponse.SC_FORBIDDEN)
                .timestamp(LocalDateTime.now())
                .build();

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
