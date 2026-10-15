package com.kashi.portal.config;

import com.kashi.portal.auth.JwtAuthenticationFilter;
import com.kashi.portal.auth.JwtService;
import com.kashi.portal.user.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserDetailsService userDetailsService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {

        // Sending 401 and 403 through the MVC exception resolver gives them the same JSON
        // shape as every other error (see GlobalExceptionHandler).
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) ->
                                exceptionResolver.resolveException(request, response, null, ex))
                        .accessDeniedHandler((request, response, ex) ->
                                exceptionResolver.resolveException(request, response, null, ex)))
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI and API docs
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/places", "/api/places/*", "/api/gallery").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/gallery/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/feedback").permitAll()
                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/places", "/api/places/*", "/api/gallery").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/feedback").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/ai/chat").permitAll()

                        // ADMIN only
                        .requestMatchers(HttpMethod.POST, "/api/places", "/api/gallery", "/api/gallery/upload").hasRole("ADMIN")
//                        .requestMatchers(HttpMethod.POST, "/api/places", "/api/gallery").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/places/*", "/api/gallery/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/places/*", "/api/gallery/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/feedback").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Everything else (for example /api/users/me) needs a valid login
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Loads a user by email. Spring Security uses this for login and the JWT filter uses it for every request. */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> userRepository.findByEmail(email)
                .map(user -> org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
