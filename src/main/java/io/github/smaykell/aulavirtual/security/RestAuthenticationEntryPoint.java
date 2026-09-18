package io.github.smaykell.aulavirtual.security;

import io.github.smaykell.aulavirtual.common.exception.CommonError;
import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiErrorWriter errorWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException {

        errorWriter.write(request, response, CommonError.UNAUTHENTICATED);
    }
}
