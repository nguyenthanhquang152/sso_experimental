package com.demo.sso.config;

import com.demo.sso.service.GoogleTokenVerifier;
import com.demo.sso.service.MicrosoftTokenVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SecurityConfig.
 * Verifies HTTP security behavior: public/protected endpoint access,
 * custom error responses, and stateless session management.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestAuthCodeStoreConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoogleTokenVerifier googleTokenVerifier;

    @MockBean
    private MicrosoftTokenVerifier microsoftTokenVerifier;

    @Nested
    @DisplayName("Public endpoints under /auth/**")
    class PublicEndpoints {

        @Test
        @DisplayName("GET /auth/providers is accessible without authentication")
        void getAuthProvidersIsPublic() throws Exception {
            mockMvc.perform(get("/auth/providers"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /auth/google/verify does not require authentication")
        void postGoogleVerifyIsPublic() throws Exception {
            mockMvc.perform(post("/auth/google/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"idToken\": \"fake\"}"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertNotEquals(401, s, "Public endpoint must not return 401");
                    assertNotEquals(403, s, "Public endpoint must not return 403");
                });
        }

        @Test
        @DisplayName("POST /auth/exchange does not require authentication")
        void postExchangeIsPublic() throws Exception {
            mockMvc.perform(post("/auth/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\": \"fake\"}"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertNotEquals(401, s, "Public endpoint must not return 401");
                    assertNotEquals(403, s, "Public endpoint must not return 403");
                });
        }

        @Test
        @DisplayName("POST /auth/logout does not require authentication")
        void postLogoutIsPublic() throws Exception {
            mockMvc.perform(post("/auth/logout"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertNotEquals(401, s, "Public endpoint must not return 401");
                    assertNotEquals(403, s, "Public endpoint must not return 403");
                });
        }
    }

    @Nested
    @DisplayName("Protected endpoints")
    class ProtectedEndpoints {

        @Test
        @DisplayName("GET /user/me returns 401 without authentication")
        void getUserMeRequiresAuthentication() throws Exception {
            mockMvc.perform(get("/user/me"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unmapped endpoint returns 401 without authentication")
        void unmappedEndpointRequiresAuthentication() throws Exception {
            mockMvc.perform(get("/api/nonexistent"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Custom authentication entry point")
    class CustomEntryPoint {

        @Test
        @DisplayName("401 response has JSON body with error message")
        void unauthorizedResponseIsJson() throws Exception {
            mockMvc.perform(get("/user/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
        }
    }

    @Nested
    @DisplayName("Stateless session management")
    class SessionManagement {

        @Test
        @DisplayName("No JSESSIONID cookie is set on responses")
        void noSessionCookie() throws Exception {
            mockMvc.perform(get("/auth/providers"))
                .andExpect(result -> {
                    String setCookie = result.getResponse().getHeader("Set-Cookie");
                    assertTrue(
                        setCookie == null || !setCookie.contains("JSESSIONID"),
                        "Stateless session policy should not produce JSESSIONID"
                    );
                });
        }
    }
}
