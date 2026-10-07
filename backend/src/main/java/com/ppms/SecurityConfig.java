package com.ppms;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(Repos.Users users) {
        return mobile -> users.findByMobile(mobile)
            .map(u -> org.springframework.security.core.userdetails.User.withUsername(u.getMobile())
                .password(u.getPasswordHash()).roles(u.getRole().name()).disabled(!"ACTIVE".equals(u.getStatus())).build())
            .orElseThrow(() -> new UsernameNotFoundException("unknown"));
    }

    @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint ep = (rq, rs, ex) -> {
            rs.setStatus(401); rs.setContentType("application/json");
            rs.getWriter().write("{\"message\":\"Invalid login credentials.\"}");
        };
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/centres/**", "/api/slots/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/staff/**").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .httpBasic(b -> b.authenticationEntryPoint(ep))
            .exceptionHandling(e -> e.authenticationEntryPoint(ep));
        return http.build();
    }
}
