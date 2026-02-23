package com.vicente.taskmanager.security.config;

import com.vicente.taskmanager.model.enums.UserRole;
import com.vicente.taskmanager.security.checker.AccountStatusUserDetailsChecker;
import com.vicente.taskmanager.security.entrypoint.AuthEntryPointJwt;
import com.vicente.taskmanager.security.entrypoint.CustomAccessDeniedHandler;
import com.vicente.taskmanager.security.filter.SecurityFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    private final SecurityFilter securityFilter;
    private final AuthEntryPointJwt authEntryPointJwt;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    private static final String[] SWAGGER = {
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/"
    };

    private static final String[] ADMIN = {
            "/api/v1/admin/users",
            "/api/v1/admin/users/*",
            "/api/v1/admin/users/*/enabled",
            "/api/v1/admin/tasks/delete/*",
            "/api/v1/admin/users/*/tasks",
            "/api/v1/admin/tasks/*"
    };

    public WebSecurityConfig(SecurityFilter securityFilter,
                             AuthEntryPointJwt authEntryPointJwt,
                             CustomAccessDeniedHandler customAccessDeniedHandler
    ) {
        this.securityFilter = securityFilter;
        this.authEntryPointJwt = authEntryPointJwt;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider provider) {
        return http.headers(headers -> headers.frameOptions(
                HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authenticationProvider(provider)
                .exceptionHandling( ex -> {
                        ex.authenticationEntryPoint(authEntryPointJwt);
                        ex.accessDeniedHandler(customAccessDeniedHandler);
                })
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize ->
                        authorize.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                                .requestMatchers(SWAGGER).permitAll()
                                .requestMatchers("/api/v1/users/me")
                                .hasAnyRole(UserRole.USER.name(), UserRole.ADMIN.name())
                                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/me/delete")
                                .hasRole(UserRole.USER.name())
                                .requestMatchers(ADMIN)
                                .hasRole(UserRole.ADMIN.name())
                                .anyRequest().authenticated()
                        // .requestMatchers("/**").access("hasRole('ADMIN') and hasRole('USER')")
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            PasswordEncoder passwordEncoder,
            AccountStatusUserDetailsChecker checker,
            UserDetailsService userDetailsService
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);
        provider.setPreAuthenticationChecks(checker);

        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }
}
