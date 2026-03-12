# Google SSO Demo — Implementation Plan

**Goal:** Build a demo app showing Google SSO with two OAuth2 flows (server-side authorization code + client-side Google Identity Services) using React, Spring Boot, PostgreSQL, and Traefik.

**Architecture:** Path-based routing via Traefik on `localhost:8000`. React SPA at `/`, Spring Boot API at `/api/`. Single origin eliminates CORS. PostgreSQL stores user records. Redis stores short-lived auth codes for the server-side OAuth2 flow. JWT for stateless auth between frontend and backend.

**Tech Stack:** React 19.2.4, Vite 7.3.1, @react-oauth/google 0.13.4, Spring Boot 3.5.11, Spring Security 6.x, PostgreSQL 18.3, Traefik 3.6.x, Java 17+, JJWT, Google API Client.

**Design doc:** `docs/plans/2026-03-11-google-sso-design.md`

---

## Task 1: Project Scaffolding

**Files:**
- Create: `.gitignore`
- Create: `.env.example`
- Create: `docker-compose.yml` (placeholder)

**Step 1: Create `.gitignore`**

```gitignore
# Java
backend/target/
backend/.mvn/wrapper/maven-wrapper.jar
*.class
*.jar
*.war

# Node
frontend/node_modules/
frontend/dist/

# IDE
.idea/
*.iml
.vscode/
*.swp

# Environment
.env

# Docker
postgres_data/

# OS
.DS_Store
Thumbs.db
```

**Step 2: Create `.env.example`**

```
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret
JWT_SECRET=change-me-to-a-random-string-at-least-32-characters-long
```

**Step 3: Commit**

```bash
git add .gitignore .env.example
git commit -m "chore: add project scaffolding (.gitignore, .env.example)"
```

---

## Task 2: Backend — Maven Project Initialization

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/demo/sso/SsoApplication.java`
- Create: `backend/src/main/resources/application.yml`

**Step 1: Create `backend/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.11</version>
        <relativePath/>
    </parent>

    <groupId>com.demo</groupId>
    <artifactId>sso</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>sso-demo</name>
    <description>Google SSO Demo with Spring Boot</description>

    <properties>
        <java.version>17</java.version>
        <jjwt.version>0.12.6</jjwt.version>
    </properties>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Security + OAuth2 Client -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-client</artifactId>
        </dependency>

        <!-- JPA + PostgreSQL -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Google API Client (for verifying ID tokens) -->
        <dependency>
            <groupId>com.google.api-client</groupId>
            <artifactId>google-api-client</artifactId>
            <version>2.7.2</version>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**Step 2: Create `backend/src/main/java/com/demo/sso/SsoApplication.java`**

```java
package com.demo.sso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SsoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SsoApplication.class, args);
    }
}
```

**Step 3: Create `backend/src/main/resources/application.yml`**

```yaml
server:
  port: 8080
  servlet:
    context-path: /api
  forward-headers-strategy: framework

spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/sso_demo}
    username: ${SPRING_DATASOURCE_USERNAME:sso_user}
    password: ${SPRING_DATASOURCE_PASSWORD:sso_pass}
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: ${SPRING_DATA_REDIS_PORT:6379}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid, profile, email

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: 86400000  # 24 hours
  frontend-url: ${FRONTEND_URL:http://localhost:8000}
  google-client-id: ${GOOGLE_CLIENT_ID}
```

**Note on `server.servlet.context-path: /api`:** This makes all Spring Boot endpoints automatically prefixed with `/api`. Spring Security OAuth2's default paths become `/api/oauth2/authorization/google` and `/api/login/oauth2/code/google`. Combined with `forward-headers-strategy: framework`, Spring Security uses the Traefik-forwarded `Host` header (`localhost`) when constructing redirect URIs.

**Step 4: Verify the project compiles**

```bash
cd backend && mvn compile -q
```
Expected: BUILD SUCCESS (no output if quiet)

**Step 5: Commit**

```bash
git add backend/
git commit -m "feat: initialize Spring Boot 3.5.11 backend project"
```

---

## Task 3: Backend — User Entity & Repository

**Files:**
- Create: `backend/src/main/java/com/demo/sso/model/User.java`
- Create: `backend/src/main/java/com/demo/sso/repository/UserRepository.java`

**Step 1: Create JPA entity `User.java`**

```java
package com.demo.sso.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_id", unique = true, nullable = false)
    private String googleId;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    @Column(name = "picture_url")
    private String pictureUrl;

    @Column(name = "login_method", length = 20)
    private String loginMethod;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastLoginAt = LocalDateTime.now();
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }

    public String getLoginMethod() { return loginMethod; }
    public void setLoginMethod(String loginMethod) { this.loginMethod = loginMethod; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
```

**Step 2: Create `UserRepository.java`**

```java
package com.demo.sso.repository;

import com.demo.sso.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByEmail(String email);
}
```

**Step 3: Verify compile**

```bash
cd backend && mvn compile -q
```

**Step 4: Commit**

```bash
git add backend/src/main/java/com/demo/sso/model/ backend/src/main/java/com/demo/sso/repository/
git commit -m "feat: add User entity and UserRepository"
```

---

## Task 4: Backend — JWT Token Service

**Files:**
- Create: `backend/src/main/java/com/demo/sso/service/JwtTokenService.java`

**Step 1: Create `JwtTokenService.java`**

This service generates and validates JWT tokens. It reads the secret and expiration from `application.yml`. It validates the secret at startup (minimum 32 chars, rejects placeholder values). Tokens include issuer (`sso-demo-backend`), audience (`sso-demo-api`), and a random `jti`.

```java
package com.demo.sso.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    private static final String ISSUER = "sso-demo-backend";
    private static final String AUDIENCE = "sso-demo-api";

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        validateSecret(secret);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    private static void validateSecret(String secret) {
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters. Set the JWT_SECRET environment variable.");
        }
        String normalized = secret.trim().toLowerCase();
        if ("default".equals(normalized) || "change-me".equals(normalized)) {
            throw new IllegalStateException(
                    "JWT secret must not be a default/placeholder value. Set the JWT_SECRET environment variable.");
        }
    }

    public String generateToken(String email, String googleId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("googleId", googleId)
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getEmailFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}
```

**Step 2: Verify compile**

```bash
cd backend && mvn compile -q
```

**Step 3: Commit**

```bash
git add backend/src/main/java/com/demo/sso/service/JwtTokenService.java
git commit -m "feat: add JWT token generation and validation"
```

---

## Task 5: Backend — UserService

**Files:**
- Create: `backend/src/main/java/com/demo/sso/service/UserService.java`

**Step 1: Create `UserService.java`**

Handles finding or creating users from OAuth2 data. Used by both flows.

```java
package com.demo.sso.service;

import com.demo.sso.model.User;
import com.demo.sso.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findOrCreateUser(String googleId, String email, String name,
                                  String pictureUrl, String loginMethod) {
        Optional<User> existing = userRepository.findByGoogleId(googleId);

        if (existing.isPresent()) {
            User user = existing.get();
            user.setName(name);
            user.setPictureUrl(pictureUrl);
            user.setLoginMethod(loginMethod);
            user.setLastLoginAt(LocalDateTime.now());
            return userRepository.save(user);
        }

        User user = new User();
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setName(name);
        user.setPictureUrl(pictureUrl);
        user.setLoginMethod(loginMethod);
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
```

**Step 2: Verify compile**

```bash
cd backend && mvn compile -q
```

**Step 3: Commit**

```bash
git add backend/src/main/java/com/demo/sso/service/UserService.java
git commit -m "feat: add UserService for find-or-create user logic"
```

---

## Task 6: Backend — Google ID Token Verifier

**Files:**
- Create: `backend/src/main/java/com/demo/sso/service/GoogleTokenVerifier.java`

**Step 1: Create `GoogleTokenVerifier.java`**

This service verifies Google ID tokens received from the client-side flow. Uses Google's official `GoogleIdTokenVerifier`.

```java
package com.demo.sso.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${app.google-client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleIdToken.Payload verify(String idTokenString) throws Exception {
        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new IllegalArgumentException("Invalid Google ID token");
        }
        return idToken.getPayload();
    }
}
```

**Step 2: Verify compile**

```bash
cd backend && mvn compile -q
```

**Step 3: Commit**

```bash
git add backend/src/main/java/com/demo/sso/service/GoogleTokenVerifier.java
git commit -m "feat: add Google ID token verification service"
```

---

## Task 7: Backend — Auth Code Store & OAuth2 Success Handler

**Files:**
- Create: `backend/src/main/java/com/demo/sso/service/AuthCodeStore.java`
- Create: `backend/src/main/java/com/demo/sso/service/RedisAuthCodeStore.java`
- Create: `backend/src/main/java/com/demo/sso/service/OAuth2SuccessHandler.java`

**Step 1: Create `AuthCodeStore.java` interface**

This interface abstracts the storage of short-lived, single-use authorization codes that map to JWTs. The server-side OAuth2 flow stores a JWT in the store and redirects with a code; the frontend exchanges the code for the JWT. This avoids exposing JWTs in URL query parameters.

```java
package com.demo.sso.service;

/**
 * Stores short-lived, single-use authorization codes that map to JWTs.
 * Codes are generated during OAuth2 success and exchanged by the frontend for a JWT.
 */
public interface AuthCodeStore {

    /**
     * Store a JWT and return a single-use authorization code.
     */
    String storeJwt(String jwt);

    /**
     * Exchange a code for the stored JWT. Returns null if invalid/expired.
     * The code is consumed and cannot be reused.
     */
    String exchangeCode(String code);
}
```

**Step 2: Create `RedisAuthCodeStore.java`**

Implements `AuthCodeStore` using Redis. Generates a 32-byte random code (URL-safe Base64), stores in Redis with key prefix `authcode:` and 30-second TTL. Exchange uses Redis `GETDEL` for atomic single-use. Excluded from test profile via `@Profile("!test")`.

```java
package com.demo.sso.service;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
@Profile("!test")
public class RedisAuthCodeStore implements AuthCodeStore {

    private static final Duration CODE_TTL = Duration.ofSeconds(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String KEY_PREFIX = "authcode:";

    private final StringRedisTemplate redisTemplate;

    public RedisAuthCodeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String storeJwt(String jwt) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        redisTemplate.opsForValue().set(KEY_PREFIX + code, jwt, CODE_TTL);
        return code;
    }

    @Override
    public String exchangeCode(String code) {
        // GETDEL: atomically get and delete — guarantees single-use
        return redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + code);
    }
}
```

**Step 3: Create `OAuth2SuccessHandler.java`**

Handles the server-side OAuth2 flow callback. When Google redirects back with the auth code and Spring Security exchanges it for user info, this handler:
1. Verifies email is verified (rejects with `?error=email_not_verified` if not)
2. Validates required attributes are present (rejects with `?error=missing_attributes` if not)
3. Extracts user attributes from the OAuth2User
4. Saves/updates the user in PostgreSQL
5. Generates a JWT
6. Stores JWT in Redis via `AuthCodeStore` and gets a single-use code
7. Redirects to the React frontend with `?code=AUTH_CODE`

```java
package com.demo.sso.service;

import com.demo.sso.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final AuthCodeStore authCodeStore;
    private final String frontendUrl;

    public OAuth2SuccessHandler(UserService userService, JwtTokenService jwtTokenService,
                                 AuthCodeStore authCodeStore,
                                 @Value("${app.frontend-url}") String frontendUrl) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.authCodeStore = authCodeStore;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        Boolean emailVerified = oAuth2User.getAttribute("email_verified");
        if (!Boolean.TRUE.equals(emailVerified)) {
            logger.warn("OAuth2 login rejected: email not verified");
            response.sendRedirect(frontendUrl + "/?error=email_not_verified");
            return;
        }

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");

        if (googleId == null || email == null) {
            logger.warn("OAuth2 login rejected: missing sub or email attribute");
            response.sendRedirect(frontendUrl + "/?error=missing_attributes");
            return;
        }

        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        User user = userService.findOrCreateUser(googleId, email, name, picture, "SERVER_SIDE");

        String jwt = jwtTokenService.generateToken(user.getEmail(), user.getGoogleId());
        String code = authCodeStore.storeJwt(jwt);

        String encodedCode = URLEncoder.encode(code, StandardCharsets.UTF_8);
        response.sendRedirect(frontendUrl + "/?code=" + encodedCode);
    }
}
```

**Step 2: Verify compile**

```bash
cd backend && mvn compile -q
```

**Step 3: Commit**

```bash
git add backend/src/main/java/com/demo/sso/service/AuthCodeStore.java backend/src/main/java/com/demo/sso/service/RedisAuthCodeStore.java backend/src/main/java/com/demo/sso/service/OAuth2SuccessHandler.java
git commit -m "feat: add auth code store and OAuth2 success handler for server-side flow"
```

---

## Task 8: Backend — Security Configuration

**Files:**
- Create: `backend/src/main/java/com/demo/sso/config/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/demo/sso/config/SecurityConfig.java`

**Step 1: Create `JwtAuthenticationFilter.java`**

A dedicated `OncePerRequestFilter` that extracts the `Authorization: Bearer <token>` header, validates the JWT via `JwtTokenService`, and sets a `UsernamePasswordAuthenticationToken` in `SecurityContextHolder` with the user's email as principal and `ROLE_USER` authority.

```java
package com.demo.sso.config;

import com.demo.sso.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

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

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (!token.isBlank() && jwtTokenService.isTokenValid(token)) {
                String email = jwtTokenService.getEmailFromToken(token);
                var auth = new UsernamePasswordAuthenticationToken(
                        email, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

**Step 2: Create `SecurityConfig.java`**

This is the core security configuration. It:
- Disables CSRF for `/auth/**` and `/user/**` endpoints (API-only paths)
- Sets session management to stateless
- Configures OAuth2 login with our custom success handler
- Injects the `JwtAuthenticationFilter` for protected endpoints
- Permits public access to auth endpoints, requires authentication for `/user/**` and all others
- Returns JSON 401 responses instead of redirecting

```java
package com.demo.sso.config;

import com.demo.sso.service.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(OAuth2SuccessHandler oAuth2SuccessHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/auth/**", "/user/**")
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
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized\"}");
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

**Note:** The `requestMatchers` paths are relative to the context path `/api`. So `/auth/**` matches `/api/auth/**` externally, `/user/**` matches `/api/user/**` externally, etc. Spring Security automatically accounts for the context path.

**Step 2: Verify compile**

```bash
cd backend && mvn compile -q
```

**Step 3: Commit**

```bash
git add backend/src/main/java/com/demo/sso/config/JwtAuthenticationFilter.java backend/src/main/java/com/demo/sso/config/SecurityConfig.java
git commit -m "feat: add Spring Security config with OAuth2 + JWT filter"
```

---

## Task 9: Backend — Controllers

**Files:**
- Create: `backend/src/main/java/com/demo/sso/controller/AuthController.java`
- Create: `backend/src/main/java/com/demo/sso/controller/UserController.java`

**Step 1: Create `AuthController.java`**

Handles the client-side flow token verification, auth code exchange (server-side flow), and logout.

```java
package com.demo.sso.controller;

import com.demo.sso.model.User;
import com.demo.sso.service.AuthCodeStore;
import com.demo.sso.service.GoogleTokenVerifier;
import com.demo.sso.service.JwtTokenService;
import com.demo.sso.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final AuthCodeStore authCodeStore;

    public AuthController(GoogleTokenVerifier googleTokenVerifier,
                           UserService userService, JwtTokenService jwtTokenService,
                           AuthCodeStore authCodeStore) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.authCodeStore = authCodeStore;
    }

    @PostMapping("/google/verify")
    public ResponseEntity<?> verifyGoogleToken(@RequestBody Map<String, String> body) {
        String credential = body.get("credential");
        if (credential == null || credential.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing credential"));
        }

        try {
            GoogleIdToken.Payload payload = googleTokenVerifier.verify(credential);

            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email not verified by Google"));
            }

            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            User user = userService.findOrCreateUser(googleId, email, name, picture, "CLIENT_SIDE");

            String jwt = jwtTokenService.generateToken(user.getEmail(), user.getGoogleId());

            return ResponseEntity.ok(Map.of("token", jwt));
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            logger.warn("Google token verification failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid Google credential"));
        }
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeCode(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing code"));
        }

        String jwt = authCodeStore.exchangeCode(code);
        if (jwt == null) {
            logger.debug("Auth code exchange failed for code: {}...",
                    code.length() > 8 ? code.substring(0, 8) : code);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired code"));
        }

        return ResponseEntity.ok(Map.of("token", jwt));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
```

**Step 2: Create `UserController.java`**

```java
package com.demo.sso.controller;

import com.demo.sso.model.User;
import com.demo.sso.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();

        return userService.findByEmail(email)
                .map(user -> ResponseEntity.ok(Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "name", user.getName() != null ? user.getName() : "",
                        "pictureUrl", user.getPictureUrl() != null ? user.getPictureUrl() : "",
                        "loginMethod", user.getLoginMethod() != null ? user.getLoginMethod() : "",
                        "createdAt", user.getCreatedAt().toString(),
                        "lastLoginAt", user.getLastLoginAt().toString()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
```

**Step 3: Verify compile**

```bash
cd backend && mvn compile -q
```

**Step 4: Commit**

```bash
git add backend/src/main/java/com/demo/sso/controller/
git commit -m "feat: add AuthController and UserController"
```

---

## Task 10: Backend — Dockerfile

**Files:**
- Create: `backend/Dockerfile`

**Step 1: Create multi-stage `backend/Dockerfile`**

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Step 2: Commit**

```bash
git add backend/Dockerfile
git commit -m "feat: add backend Dockerfile (multi-stage Maven build)"
```

---

## Task 11: Frontend — Vite + React Project Scaffolding

**Files:**
- Create: `frontend/` project via Vite scaffolding
- Modify: `frontend/package.json` (add dependencies)
- Modify: `frontend/vite.config.ts` (configure for Docker)

**Step 1: Scaffold Vite + React + TypeScript project**

```bash
cd /home/nt-quang/Workspaces/personal/sso_experimental
npm create vite@latest frontend -- --template react-ts
```

**Step 2: Install dependencies**

```bash
cd frontend
npm install
npm install @react-oauth/google react-router-dom
```

**Step 3: Configure `frontend/vite.config.ts`**

Replace the generated config with:

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
  },
})
```

**Note:** `host: '0.0.0.0'` is required so the Vite dev server listens on all interfaces when running locally. `strictPort: true` prevents Vite from using a different port if 5173 is occupied. In Docker, the production build uses nginx instead of the Vite dev server.

**Step 4: Clean up default Vite files**

Remove default CSS and assets that won't be used:
- Delete `frontend/src/App.css`
- Delete `frontend/src/index.css`
- Delete `frontend/src/assets/react.svg`
- Delete `frontend/public/vite.svg`

**Step 5: Verify dev server starts**

```bash
cd frontend && npm run dev -- --host &
sleep 3 && curl -s http://localhost:5173 | head -5
kill %1
```
Expected: HTML response from Vite dev server

**Step 6: Commit**

```bash
git add frontend/
git commit -m "feat: scaffold React + Vite + TypeScript frontend"
```

---

## Task 12: Frontend — API Client & Auth Hook

**Files:**
- Create: `frontend/src/api/client.ts`
- Create: `frontend/src/hooks/useAuth.ts`

**Step 1: Create `frontend/src/api/client.ts`**

Thin fetch wrapper that attaches JWT to requests.

```typescript
const API_BASE = '/api';

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('jwt');

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    throw new Error(`API error: ${response.status}`);
  }

  return response.json();
}
```

**Step 2: Create `frontend/src/hooks/useAuth.ts`**

```typescript
import { useState, useEffect, useCallback } from 'react';
import { apiFetch } from '../api/client';

interface UserProfile {
  id: number;
  email: string;
  name: string;
  pictureUrl: string;
  loginMethod: string;
  createdAt: string;
  lastLoginAt: string;
}

export function useAuth() {
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem('jwt')
  );
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(false);

  const login = useCallback((jwt: string) => {
    localStorage.setItem('jwt', jwt);
    setToken(jwt);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('jwt');
    setToken(null);
    setUser(null);
  }, []);

  useEffect(() => {
    if (!token) {
      setUser(null);
      return;
    }

    setLoading(true);
    apiFetch<UserProfile>('/user/me')
      .then(setUser)
      .catch(() => {
        // Token is invalid or expired
        logout();
      })
      .finally(() => setLoading(false));
  }, [token, logout]);

  return { token, user, loading, login, logout, isAuthenticated: !!token };
}
```

**Step 3: Commit**

```bash
git add frontend/src/api/ frontend/src/hooks/
git commit -m "feat: add API client with JWT and useAuth hook"
```

---

## Task 13: Frontend — Login Components

**Files:**
- Create: `frontend/src/components/ServerSideLogin.tsx`
- Create: `frontend/src/components/ClientSideLogin.tsx`

**Step 1: Create `ServerSideLogin.tsx`**

This component simply renders a link that navigates to the Spring Security OAuth2 authorization endpoint. The browser leaves the SPA and follows the full server-side redirect flow.

```tsx
export function ServerSideLogin() {
  return (
    <div style={{
      border: '1px solid #e0e0e0',
      borderRadius: '8px',
      padding: '24px',
      maxWidth: '400px',
    }}>
      <h2>Server-Side Flow</h2>
      <p style={{ color: '#666', fontSize: '14px' }}>
        The browser redirects to Google via the Spring Boot backend.
        Spring Security handles the entire OAuth2 authorization code exchange.
      </p>
      <a
        href="/api/oauth2/authorization/google"
        style={{
          display: 'inline-block',
          padding: '10px 24px',
          backgroundColor: '#4285f4',
          color: 'white',
          textDecoration: 'none',
          borderRadius: '4px',
          fontWeight: 'bold',
        }}
      >
        Sign in with Google (Server-Side)
      </a>
    </div>
  );
}
```

**Step 2: Create `ClientSideLogin.tsx`**

This component uses `@react-oauth/google` to render the Google Sign-In button. When the user signs in, it receives a credential (ID token) and sends it to the backend for verification.

```tsx
import { GoogleLogin, CredentialResponse } from '@react-oauth/google';
import { apiFetch } from '../api/client';

interface ClientSideLoginProps {
  onSuccess: (token: string) => void;
}

export function ClientSideLogin({ onSuccess }: ClientSideLoginProps) {
  const handleSuccess = async (credentialResponse: CredentialResponse) => {
    if (!credentialResponse.credential) return;

    try {
      const data = await apiFetch<{ token: string }>('/auth/google/verify', {
        method: 'POST',
        body: JSON.stringify({ credential: credentialResponse.credential }),
      });
      onSuccess(data.token);
    } catch (error) {
      console.error('Login failed:', error);
    }
  };

  return (
    <div style={{
      border: '1px solid #e0e0e0',
      borderRadius: '8px',
      padding: '24px',
      maxWidth: '400px',
    }}>
      <h2>Client-Side Flow</h2>
      <p style={{ color: '#666', fontSize: '14px' }}>
        Google Sign-In happens directly in the browser via a popup.
        The ID token is sent to the backend for verification.
      </p>
      <GoogleLogin
        onSuccess={handleSuccess}
        onError={() => console.error('Google Login Failed')}
      />
    </div>
  );
}
```

**Step 3: Commit**

```bash
git add frontend/src/components/
git commit -m "feat: add ServerSideLogin and ClientSideLogin components"
```

---

## Task 14: Frontend — Pages & App Routing

**Files:**
- Create: `frontend/src/pages/HomePage.tsx`
- Create: `frontend/src/pages/DashboardPage.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/main.tsx`

**Step 1: Create `HomePage.tsx`**

The landing page with two login options. Also checks URL for `?code=` (from server-side flow redirect) and exchanges it for a JWT via `POST /api/auth/exchange`.

```tsx
import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ServerSideLogin } from '../components/ServerSideLogin';
import { ClientSideLogin } from '../components/ClientSideLogin';
import { useAuth } from '../hooks/useAuth';
import { apiFetch } from '../api/client';

export function HomePage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const code = searchParams.get('code');
    if (code) {
      // Strip code from URL immediately to prevent leakage
      window.history.replaceState({}, '', '/');

      // Exchange the single-use code for a JWT
      apiFetch<{ token: string }>('/auth/exchange', {
        method: 'POST',
        body: JSON.stringify({ code }),
      })
        .then((data) => {
          login(data.token);
          navigate('/dashboard', { replace: true });
        })
        .catch(() => {
          // Code was invalid or expired — stay on homepage
        });
    }
  }, [searchParams, login, navigate]);

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const handleClientLogin = (token: string) => {
    login(token);
    navigate('/dashboard');
  };

  return (
    <div style={{ maxWidth: '900px', margin: '0 auto', padding: '40px 20px' }}>
      <h1>Google SSO Demo</h1>
      <p style={{ color: '#666', marginBottom: '32px' }}>
        This demo shows two Google OAuth2 flows side-by-side.
        Choose either method to sign in.
      </p>
      <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap' }}>
        <ServerSideLogin />
        <ClientSideLogin onSuccess={handleClientLogin} />
      </div>
    </div>
  );
}
```

**Step 2: Create `DashboardPage.tsx`**

Shows the authenticated user's Google profile info.

```tsx
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useEffect } from 'react';

export function DashboardPage() {
  const { user, loading, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  if (loading) {
    return <div style={{ padding: '40px', textAlign: 'center' }}>Loading...</div>;
  }

  if (!user) {
    return null;
  }

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto', padding: '40px 20px' }}>
      <h1>Dashboard</h1>
      <div style={{
        border: '1px solid #e0e0e0',
        borderRadius: '8px',
        padding: '24px',
        marginTop: '20px',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '20px' }}>
          {user.pictureUrl && (
            <img
              src={user.pictureUrl}
              alt={user.name}
              style={{ width: '64px', height: '64px', borderRadius: '50%' }}
            />
          )}
          <div>
            <h2 style={{ margin: 0 }}>{user.name}</h2>
            <p style={{ margin: '4px 0', color: '#666' }}>{user.email}</p>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '14px' }}>
          <div>
            <strong>Login Method: </strong>
            <span style={{
              padding: '2px 8px',
              borderRadius: '4px',
              backgroundColor: user.loginMethod === 'SERVER_SIDE' ? '#e3f2fd' : '#f3e5f5',
              color: user.loginMethod === 'SERVER_SIDE' ? '#1565c0' : '#7b1fa2',
              fontWeight: 'bold',
              fontSize: '12px',
            }}>
              {user.loginMethod}
            </span>
          </div>
          <div>
            <strong>Last Login: </strong>
            {new Date(user.lastLoginAt).toLocaleString()}
          </div>
          <div>
            <strong>Account Created: </strong>
            {new Date(user.createdAt).toLocaleString()}
          </div>
        </div>

        <button
          onClick={handleLogout}
          style={{
            marginTop: '24px',
            padding: '10px 24px',
            backgroundColor: '#f44336',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontWeight: 'bold',
          }}
        >
          Logout
        </button>
      </div>
    </div>
  );
}
```

**Step 3: Update `App.tsx`**

```tsx
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { GoogleOAuthProvider } from '@react-oauth/google';
import { HomePage } from './pages/HomePage';
import { DashboardPage } from './pages/DashboardPage';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

function App() {
  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/dashboard" element={<DashboardPage />} />
        </Routes>
      </BrowserRouter>
    </GoogleOAuthProvider>
  );
}

export default App;
```

**Step 4: Update `main.tsx`**

```tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
```

**Step 5: Commit**

```bash
git add frontend/src/
git commit -m "feat: add pages, routing, and GoogleOAuthProvider setup"
```

---

## Task 15: Frontend — Dockerfile, Nginx & Environment

**Files:**
- Create: `frontend/Dockerfile`
- Create: `frontend/nginx.conf`
- Create: `frontend/.env.example`

**Step 1: Create `frontend/Dockerfile`**

Multi-stage build: builds the React app with Node, then serves it with nginx.

```dockerfile
# Stage 1: Build
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Serve with nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
RUN chown -R nginx:nginx /usr/share/nginx/html
USER nginx
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**Step 2: Create `frontend/nginx.conf`**

Simple config for SPA routing — all requests fall back to `index.html`.

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

**Step 2: Create `frontend/.env.example`**

```
VITE_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
```

**Note:** Vite exposes environment variables prefixed with `VITE_` to the client bundle. The Google Client ID is not a secret — it's publicly visible in the browser anyway.

**Step 4: Commit**

```bash
git add frontend/Dockerfile frontend/nginx.conf frontend/.env.example
git commit -m "feat: add frontend Dockerfile (nginx), nginx.conf, and env example"
```

---

## Task 16: Docker Compose & Traefik

**Files:**
- Create: `docker-compose.yml`
- Update: `.env.example` (add frontend env var)

**Step 1: Create `docker-compose.yml`**

```yaml
services:
  traefik:
    image: traefik:v3.6
    command:
      - "--providers.docker=true"
      - "--providers.docker.exposedbydefault=false"
      - "--entrypoints.web.address=:8000"
    ports:
      - "8000:8000"
    volumes:
      - ${DOCKER_SOCK:-/run/user/1000/docker.sock}:/var/run/docker.sock:ro

  frontend:
    build: ./frontend
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.frontend.rule=Host(`localhost`)"
      - "traefik.http.routers.frontend.entrypoints=web"
      - "traefik.http.routers.frontend.priority=1"
      - "traefik.http.services.frontend.loadbalancer.server.port=80"

  backend:
    build: ./backend
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/${POSTGRES_DB:-sso_demo}
      - SPRING_DATASOURCE_USERNAME=${POSTGRES_USER:-sso_user}
      - SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD:-sso_pass}
      - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
      - JWT_SECRET=${JWT_SECRET}
      - FRONTEND_URL=http://localhost:8000
      - SPRING_DATA_REDIS_HOST=redis
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.backend.rule=Host(`localhost`) && PathPrefix(`/api`)"
      - "traefik.http.routers.backend.entrypoints=web"
      - "traefik.http.routers.backend.priority=2"
      - "traefik.http.services.backend.loadbalancer.server.port=8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy

  redis:
    image: redis:7-alpine
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 5

  postgres:
    image: postgres:18-alpine
    environment:
      - POSTGRES_DB=${POSTGRES_DB:-sso_demo}
      - POSTGRES_USER=${POSTGRES_USER:-sso_user}
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-sso_pass}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-sso_user} -d ${POSTGRES_DB:-sso_demo}"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

**Step 2: Update `.env.example`**

```
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret
JWT_SECRET=change-me-to-a-random-string-at-least-32-characters-long
```

**Step 3: Commit**

```bash
git add docker-compose.yml .env.example
git commit -m "feat: add Docker Compose with Traefik, backend, frontend, Postgres, Redis"
```

---

## Task 17: End-to-End Smoke Test

**Prerequisites:** You must have a Google Cloud project with OAuth2 credentials configured:
- Authorised JavaScript origins: `http://localhost:8000`
- Authorised redirect URIs: `http://localhost:8000/api/login/oauth2/code/google`

**Step 1: Create `.env` from `.env.example` and fill in real credentials**

```bash
cp .env.example .env
# Edit .env with your Google credentials and a random JWT secret
```

**Step 2: Start all services**

```bash
docker compose up --build -d
```
Expected: All 5 containers start (traefik, frontend, backend, postgres, redis)

**Step 3: Verify services are healthy**

```bash
docker compose ps
```
Expected: All services showing "Up" / "healthy"

**Step 4: Test Traefik routing**

```bash
curl -s http://localhost:8000/ | head -5
```
Expected: HTML from nginx (React production build)

```bash
curl -s http://localhost:8000/api/auth/logout -X POST
```
Expected: `{"message":"Logged out successfully"}`

**Step 5: Test in browser**

1. Open `http://localhost:8000` in your browser
2. You should see the Home page with two login options
3. Click "Sign in with Google (Server-Side)" — you should be redirected to Google consent
4. After consent, you should land on `/dashboard` with your profile displayed
5. Click Logout, go back to Home
6. Click the Google Sign-In button (Client-Side) — a popup should appear
7. After consent, you should land on `/dashboard` again, this time with "CLIENT_SIDE" badge

**Step 6: Verify database records**

```bash
docker compose exec postgres psql -U sso_user -d sso_demo -c "SELECT id, email, login_method, last_login_at FROM users;"
```
Expected: Your user record(s) with the correct login method

**Step 7: Final commit (if any adjustments were needed)**

```bash
git add -A
git commit -m "chore: final adjustments from smoke testing"
```

---

## Summary

| Task | Description | Key Files |
|---|---|---|
| 1 | Project scaffolding | `.gitignore`, `.env.example` |
| 2 | Backend Maven project | `pom.xml`, `SsoApplication.java`, `application.yml` |
| 3 | User entity + repository | `User.java`, `UserRepository.java` |
| 4 | JWT token service | `JwtTokenService.java` |
| 5 | UserService | `UserService.java` |
| 6 | Google token verifier | `GoogleTokenVerifier.java` |
| 7 | Auth code store + OAuth2 success handler | `AuthCodeStore.java`, `RedisAuthCodeStore.java`, `OAuth2SuccessHandler.java` |
| 8 | Security configuration | `JwtAuthenticationFilter.java`, `SecurityConfig.java` |
| 9 | Controllers | `AuthController.java`, `UserController.java` |
| 10 | Backend Dockerfile | `backend/Dockerfile` |
| 11 | Frontend scaffolding | Vite + React + deps |
| 12 | API client + auth hook | `client.ts`, `useAuth.ts` |
| 13 | Login components | `ServerSideLogin.tsx`, `ClientSideLogin.tsx` |
| 14 | Pages + routing | `HomePage.tsx`, `DashboardPage.tsx`, `App.tsx` |
| 15 | Frontend Dockerfile + nginx | `frontend/Dockerfile`, `nginx.conf` |
| 16 | Docker Compose + Traefik | `docker-compose.yml` |
| 17 | End-to-end smoke test | Manual test checklist |
