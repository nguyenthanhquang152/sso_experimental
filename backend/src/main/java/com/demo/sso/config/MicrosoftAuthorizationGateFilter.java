package com.demo.sso.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MicrosoftAuthorizationGateFilter extends OncePerRequestFilter {

    private final AuthRolloutProperties rolloutProperties;
    private final String frontendUrl;

    public MicrosoftAuthorizationGateFilter(
            AuthRolloutProperties rolloutProperties,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.rolloutProperties = rolloutProperties;
        this.frontendUrl = frontendUrl;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.endsWith("/oauth2/authorization/microsoft") && !rolloutProperties.getMicrosoft().isServerSideEnabled()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Microsoft server-side login is disabled\"}");
            return;
        }

        if (path.endsWith("/login/oauth2/code/microsoft") && !rolloutProperties.getMicrosoft().isServerSideEnabled()) {
            SecurityContextHolder.clearContext();
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
            response.sendRedirect(frontendUrl + "/?error=provider_disabled&provider=microsoft&flow=server_side");
            return;
        }

        filterChain.doFilter(request, response);
    }
}