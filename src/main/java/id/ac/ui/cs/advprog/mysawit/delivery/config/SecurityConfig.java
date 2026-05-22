package id.ac.ui.cs.advprog.mysawit.delivery.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    /**
     * Prevent Spring Boot from registering JwtFilter as a standalone servlet filter.
     * It must only run inside the Spring Security filter chain (added via addFilterBefore
     * below). Without this, JwtFilter runs twice: once before FilterChainProxy sets up
     * the SecurityContext, and once inside the chain — but OncePerRequestFilter skips
     * the second run, leaving the SecurityContext empty after SecurityContextHolderFilter
     * resets it.
     */
    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter jwtFilter) {
        FilterRegistrationBean<JwtFilter> registration =
                new FilterRegistrationBean<>(jwtFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/v1/deliveries/my-plantation")
                                .hasRole("MANDOR")
                        .requestMatchers("/api/v1/deliveries/drivers/available")
                                .hasRole("MANDOR")
                        .requestMatchers("/api/v1/deliveries/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/deliveries/me/mandor/**").hasRole("MANDOR")
                        .requestMatchers("/api/v1/deliveries/me/**").hasRole("SUPIR")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
