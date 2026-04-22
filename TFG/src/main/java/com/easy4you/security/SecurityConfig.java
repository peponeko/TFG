package com.easy4you.security;

import com.easy4you.exception.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtFilter jwtFilter;
  private final UserDetailsService userDetailsService;
  private final ObjectMapper objectMapper;

  @Bean
  @Order(1)
  public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {

    http
        .securityMatcher(new OrRequestMatcher(
            new AntPathRequestMatcher("/admin/**"),
            new AntPathRequestMatcher("/dev/**"),
            new AntPathRequestMatcher("/login"),
            new AntPathRequestMatcher("/logout")
        ))
        .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(cspDevPolicy())))
        .authenticationProvider(authenticationProvider())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        new AntPathRequestMatcher("/login"),
                        new AntPathRequestMatcher("/dev/**"),
                        new AntPathRequestMatcher("/css/**"),
                        new AntPathRequestMatcher("/js/**"),
                        new AntPathRequestMatcher("/images/**"),
                        new AntPathRequestMatcher("/error"))
                    .permitAll()
                    // Admin endpoints require ADMIN role (hardened from previous permitAll)
                    .requestMatchers(new AntPathRequestMatcher("/admin/**"))
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .formLogin(
            form ->
                form.loginPage("/login")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/admin", true)
                    .permitAll())
        .logout(logout -> logout
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
            .logoutSuccessUrl("/login?logout")
            .permitAll());

    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {

    http
        .securityMatcher(new AntPathRequestMatcher("/api/**"))
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(cspDevPolicy())))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> 
                writeError(request, response, HttpStatus.UNAUTHORIZED, "No autenticado o token inválido"))
            .accessDeniedHandler((request, response, accessDeniedException) -> 
                writeError(request, response, HttpStatus.FORBIDDEN, "Acceso denegado"))
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/usuarios/**")).hasRole("ADMIN")
            .anyRequest().authenticated()
        );

    return http.build();
  }

  @Bean
  @Order(3)
  public SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher(new AntPathRequestMatcher("/app/**"))
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(cspDevPolicy())))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) ->
                response.sendRedirect("/app/login?expired=1"))
        )
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(new AntPathRequestMatcher("/app/login"))
                    .permitAll()
                    .anyRequest()
                    .authenticated());

    return http.build();
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  private void writeError(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
      throws IOException {

    ApiErrorResponse body = new ApiErrorResponse(
            LocalDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            null
    );

    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), body);
  }

  private String cspDevPolicy() {
    return String.join(
        "; ",
        "default-src 'self'",
        "base-uri 'self'",
        "object-src 'none'",
        "img-src 'self' data:",
        "font-src 'self' https://fonts.gstatic.com data:",
        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
        "script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com",
        "connect-src 'self'");
  }
}
