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
                        "/dashboard.html",
                        "/exam.html",
                        "/student/studentdashboard.html",
                        "/student/my-courses.html",
                        "/student/study-material.html",
                        "/faculty/exam-approval-requests.html",
                        "/student/**",
                        "/faculty/**",
                        "/admin/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico"
                ).permitAll()

                // Authentication endpoints.
                .requestMatchers("/api/auth/**").permitAll()

                // Public APIs.
                .requestMatchers("/api/public/**").permitAll()

                .requestMatchers(
                        HttpMethod.GET,
                        "/api/courses/**"
                ).permitAll()

                .requestMatchers("/api/student/courses/**").permitAll()
                .requestMatchers("/api/materials/course/**").permitAll()
                .requestMatchers("/api/materials/**").permitAll()
                // .requestMatchers("/api/exams/**").permitAll()
                // .requestMatchers("/api/questions/exam/**").permitAll()
                .requestMatchers("/api/student/dashboard/**").permitAll()
                .requestMatchers("/api/results/attempt/**").permitAll()
                .requestMatchers("/api/results/student/**").permitAll()
                .requestMatchers("/api/coding-challenges/**").permitAll()
                .requestMatchers("/api/coding-challenge-progress/**").permitAll()
                .requestMatchers("/api/exam-eligibility/**").permitAll()
              .requestMatchers(
        org.springframework.http.HttpMethod.POST,
        "/api/exam-approval/request"
).hasRole("STUDENT")

.requestMatchers(
        org.springframework.http.HttpMethod.GET,
        "/api/exam-approval/student/**"
).hasRole("STUDENT")

.requestMatchers(
        "/api/exam-approval/pending",
        "/api/exam-approval/*/approve",
        "/api/exam-approval/*/reject"
).hasRole("FACULTY")

                // Student progress requires the STUDENT role.
                .requestMatchers("/api/material-progress/**")
                .hasRole("STUDENT")

                // Admin endpoints require ADMIN role.
                .requestMatchers("/api/admin/**")
                .hasRole("ADMIN")

                // Faculty endpoints require FACULTY role.
                .requestMatchers("/api/faculty/**")
                .hasRole("FACULTY")

                // Student endpoints require STUDENT role.
                .requestMatchers("/api/student/**")
                .hasRole("STUDENT")

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