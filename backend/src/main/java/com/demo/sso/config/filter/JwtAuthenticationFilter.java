package com.demo.sso.config.filter;

import com.demo.sso.service.model.AuthenticatedUserIdentity;
import com.demo.sso.service.token.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String ROLE_USER = "ROLE_USER";

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<AuthenticatedUserIdentity> identity = jwtTokenService.validateAndExtract(token);
        if (identity.isEmpty()) {
            logger.warn("Invalid JWT token presented on {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken(
                identity.get(), null,
                List.of(new SimpleGrantedAuthority(ROLE_USER)));
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
