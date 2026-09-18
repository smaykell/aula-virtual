package io.github.smaykell.aulavirtual.security;

import io.github.smaykell.aulavirtual.common.exception.CommonError;
import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiErrorWriter errorWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        errorWriter.write(request, response, CommonError.ACCESS_DENIED);
    }
}
