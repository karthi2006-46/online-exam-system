package com.lms.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true,
        prePostEnabled = true
)
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http,
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService) throws Exception {

        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder)
                .and()
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http
                // Enable CORS using WebConfig.corsConfigurationSource().
                .cors()
                .and()

                // Disable CSRF for the stateless JWT API.
                .csrf()
                .disable()

                // Configure stateless authentication.
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

                // Configure authorization rules.
                .authorizeRequests()

                // Public frontend pages and static resources.
                .requestMatchers(
                        "/",
                        "/login.html",
                        "/signup.html",
                        "/forgot-password.html",
                        "/dashboard.html",
                        "/exam.html",
                        "/student/**",
                        "/faculty/**",
                        "/admin/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico",
                        "/error"
                ).permitAll()

                // Only these authentication endpoints are public.
                // Profile and password endpoints need a valid JWT.
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/signup",
                        "/api/auth/login",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password"
                ).permitAll()

                .requestMatchers("/api/auth/**").authenticated()

                // Public APIs.
                .requestMatchers("/api/public/**").permitAll()

                // Public course catalogue (used by the signup page).
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/courses/active",
                        "/api/courses/*"
                ).permitAll()

                // Role-protected areas are declared before broader rules so that
                // they can never be shadowed by a previous permitAll().
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/faculty/**").hasRole("FACULTY")
                .requestMatchers("/api/student/**").hasRole("STUDENT")

                // Student progress requires the STUDENT role.
                .requestMatchers("/api/material-progress/**").hasRole("STUDENT")

                // Exam approval flow.
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/exam-approval/request"
                ).hasRole("STUDENT")

                .requestMatchers(
                        HttpMethod.GET,
                        "/api/exam-approval/student/**"
                ).hasRole("STUDENT")

                .requestMatchers(
                        "/api/exam-approval/pending",
                        "/api/exam-approval/*/approve",
                        "/api/exam-approval/*/reject"
                ).hasRole("FACULTY")

                // Coding challenge authoring is faculty/admin only.
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/coding-challenges/**"
                ).hasAnyRole("FACULTY", "ADMIN")

                .requestMatchers(
                        HttpMethod.PUT,
                        "/api/coding-challenges/**"
                ).hasAnyRole("FACULTY", "ADMIN")

                .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/coding-challenges/**"
                ).hasAnyRole("FACULTY", "ADMIN")

                .requestMatchers(
                        HttpMethod.DELETE,
                        "/api/coding-challenges/**"
                ).hasAnyRole("FACULTY", "ADMIN")

                // Challenge reads now require a logged-in user.
                .requestMatchers("/api/coding-challenges/**").authenticated()
                .requestMatchers("/api/coding-challenge-progress/**").authenticated()
                .requestMatchers("/api/exam-eligibility/**").authenticated()
                .requestMatchers("/api/materials/**").authenticated()
                .requestMatchers("/api/results/**").authenticated()

                // Reports are authenticated; the controllers add role checks.
                .requestMatchers("/api/reports/**").authenticated()

                // All remaining requests require authentication.
                .anyRequest()
                .authenticated()

                .and()

                // Register the JWT authentication filter.
                .addFilterBefore(
                        jwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}