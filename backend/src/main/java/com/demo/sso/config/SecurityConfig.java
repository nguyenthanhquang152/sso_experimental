package com.demo.sso.config;

import com.demo.sso.config.filter.JwtAuthenticationFilter;
import com.demo.sso.config.filter.MicrosoftAuthorizationGateFilter;
import com.demo.sso.dto.ErrorResponse;
import com.demo.sso.service.auth.OAuth2SuccessHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String AUTH_GOOGLE_VERIFY_PATH = "/auth/google/verify";
    private static final String AUTH_MICROSOFT_PATH = "/auth/microsoft/**";
    private static final String AUTH_EXCHANGE_PATH = "/auth/exchange";
    private static final String AUTH_LOGOUT_PATH = "/auth/logout";

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MicrosoftAuthorizationGateFilter microsoftAuthorizationGateFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(OAuth2SuccessHandler oAuth2SuccessHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          MicrosoftAuthorizationGateFilter microsoftAuthorizationGateFilter,
                          ObjectMapper objectMapper) {
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.microsoftAuthorizationGateFilter = microsoftAuthorizationGateFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    AUTH_GOOGLE_VERIFY_PATH,
                    AUTH_MICROSOFT_PATH,
                    AUTH_EXCHANGE_PATH,
                    AUTH_LOGOUT_PATH
                )
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/auth/**"
                ).permitAll()
                .requestMatchers("/user/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2SuccessHandler)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(this::handleUnauthorized)
            )
            .addFilterBefore(microsoftAuthorizationGateFilter, OAuth2AuthorizationRequestRedirectFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void handleUnauthorized(HttpServletRequest request,
                                     HttpServletResponse response,
                                     AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse("Unauthorized")));
    }
}
