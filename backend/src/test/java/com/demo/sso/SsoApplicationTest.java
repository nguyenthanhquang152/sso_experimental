package com.demo.sso;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Smoke test for {@link SsoApplication}. Verifies the main class is loadable.
 * Full application context startup is covered by the @SpringBootTest integration tests.
 */
class SsoApplicationTest {

    @Test
    void mainClassExists() {
        assertDoesNotThrow(() -> Class.forName("com.demo.sso.SsoApplication"),
            "SsoApplication class should be loadable");
    }
}
