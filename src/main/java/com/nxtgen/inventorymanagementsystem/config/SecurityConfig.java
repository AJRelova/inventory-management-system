package com.nxtgen.inventorymanagementsystem.config;

import com.nxtgen.inventorymanagementsystem.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .userDetailsService(customUserDetailsService)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/style.css", "/app.js", "/logo.png", "/logo2.png").permitAll()
                        .requestMatchers("/api/auth/me").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/items/**").hasAnyRole("ADMIN", "STAFF", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/items/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/items/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/items/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/history/**").hasAnyRole("ADMIN", "STAFF", "VIEWER")

                        .requestMatchers(HttpMethod.GET, "/api/import/excel").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/import/excel").hasAnyRole("ADMIN", "STAFF")

                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}