package io.github.smaykell.aulavirtual.common.web;

import io.github.smaykell.aulavirtual.common.dto.ApiError;
import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiErrorWriter {

    private final JsonMapper jsonMapper;

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code)
            throws IOException {

        ApiError error = ApiError.of(code, request.getRequestURI());
        log.debug("[{}] {} {} -> {} {}: {}", error.traceId(), request.getMethod(),
                request.getRequestURI(), error.status(), error.code(), error.message());

        response.setStatus(error.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), error);
    }
}
