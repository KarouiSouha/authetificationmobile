package com.healthapp.doctor.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenValidator jwtTokenValidator;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            .authorizeHttpRequests(auth -> auth
                // ✅ Routes publiques (pas d'authentification)
                .requestMatchers(
                    "/api/doctors/register",
                    "/api/doctors/login",
                    "/api/doctors/available",
                    "/api/doctors/{id}",
                    "/api/doctors/{id}/available-slots",
                    "/api/doctors/appointments/{id}",
                    "/actuator/**",
                    "/error"
                ).permitAll()
                
                // ✅ WebRTC - Accessible aux DOCTOR et USER (patients)
                .requestMatchers("/api/webrtc/**")
                    .hasAnyRole("DOCTOR", "USER")
                
                // ✅ Appointments - Accessible aux deux rôles
                .requestMatchers(
                    "/api/doctors/appointments",
                    "/api/doctors/appointments/patient/**"
                ).hasAnyRole("DOCTOR", "USER")
                
                // ✅ Routes réservées aux docteurs
                .requestMatchers(
                    "/api/doctors/profile",
                    "/api/doctors/update-profile",
                    "/api/doctors/available-slots/**",
                    "/api/doctors/appointments/doctor/**",
                    "/api/doctors/appointments/*/complete",
                    "/api/doctors/appointments/*/cancel"
                ).hasRole("DOCTOR")
                
                // Tout le reste nécessite une authentification
                .anyRequest().authenticated()
            )
            
            .addFilterBefore(jwtTokenValidator, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}