package io.github.smaykell.aulavirtual.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica la peticion a partir del token JWT de la cabecera Authorization.
 *
 * <p>La autenticacion se construye con los claims del token, sin consultar la base
 * de datos: el token es la unica fuente de verdad de la sesion.
 *
 * <p>Un token ausente o invalido no corta la cadena; simplemente deja la peticion
 * sin autenticar y es la configuracion de seguridad quien decide si ese recurso
 * era publico o si responde 401.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = extraerToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticar(token, request);
        }
        chain.doFilter(request, response);
    }

    private void autenticar(String token, HttpServletRequest request) {
        try {
            Claims claims = jwtService.validar(token).getPayload();
            List<SimpleGrantedAuthority> authorities = jwtService.extraerAuthorities(claims).stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            var autenticacion = new UsernamePasswordAuthenticationToken(
                    claims.getSubject(), null, authorities);
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            log.debug("Token rechazado en {} {}: {}", request.getMethod(), request.getRequestURI(),
                    ex.getMessage());
        }
    }

    private String extraerToken(HttpServletRequest request) {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.startsWith(PREFIJO_BEARER)) {
            return null;
        }
        String token = cabecera.substring(PREFIJO_BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
