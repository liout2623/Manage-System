package com.example.demo.config;

import com.example.demo.security.JsonAccessDeniedHandler;
import com.example.demo.security.JsonAuthenticationEntryPoint;
import com.example.demo.security.JwtAuthFilter;
import com.example.demo.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@EnableMethodSecurity
public class SecurityConfig {

    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JsonAuthenticationEntryPoint authenticationEntryPoint,
                          JsonAccessDeniedHandler accessDeniedHandler) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/error"
                ).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // ── 用户管理：仅 ADMIN ──
                .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/me").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/users/export").hasRole("ADMIN")
                // ── 客户管理：敏感操作仅 ADMIN ──
                .requestMatchers(HttpMethod.POST, "/api/customers/import").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/customers/export").hasRole("ADMIN")
                // ── 服务项目管理：GET 公开（官网大屏），其余仅 ADMIN ──
                .requestMatchers(HttpMethod.GET, "/api/services/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/services/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/services/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/services/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/services/**").hasRole("ADMIN")
                // ── 理疗师-服务项目关联管理 ──
                .requestMatchers(HttpMethod.GET, "/api/therapist-services/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/therapist-services").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/therapist-services/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration c = new CorsConfiguration();
            c.setAllowedOrigins(List.of("http://localhost:5173"));
            c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
            c.setAllowedHeaders(List.of("*"));
            c.setAllowCredentials(true);
            return c;
        };
    }
}