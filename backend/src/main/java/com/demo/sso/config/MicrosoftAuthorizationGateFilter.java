package com.demo.sso.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MicrosoftAuthorizationGateFilter extends OncePerRequestFilter {

    private static final RequestMatcher AUTHORIZATION_MATCHER =
            new AntPathRequestMatcher("/**/oauth2/authorization/microsoft");
    private static final RequestMatcher CALLBACK_MATCHER =
            new AntPathRequestMatcher("/**/login/oauth2/code/microsoft");

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

        if (rolloutProperties.getMicrosoft().isServerSideEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (AUTHORIZATION_MATCHER.matches(request)) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Microsoft server-side login is disabled\"}");
            return;
        }

        if (CALLBACK_MATCHER.matches(request)) {
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