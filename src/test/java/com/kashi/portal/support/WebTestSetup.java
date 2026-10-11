package com.kashi.portal.support;

import com.kashi.portal.auth.JwtService;
import com.kashi.portal.config.CorsConfig;
import com.kashi.portal.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Put next to @WebMvcTest so the real security rules, JWT filter and CORS settings are active.
 * The test class must declare a @MockitoBean UserRepository, which the security setup needs.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({SecurityConfig.class, CorsConfig.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=" + TestJwt.SECRET,
        "app.jwt.expiration-ms=3600000",
        "app.cors.allowed-origins=http://localhost:5173"
})
public @interface WebTestSetup {
}
