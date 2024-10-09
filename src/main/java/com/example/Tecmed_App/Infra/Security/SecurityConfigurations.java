package com.example.Tecmed_App.Infra.Security;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // Habilita o uso das anotações @PreAuthorize e
public class SecurityConfigurations {
    @Autowired
    private SecurityFilter securityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(req -> {
                req.requestMatchers(HttpMethod.POST, "/api/v1/user/authenticate").permitAll();
                req.requestMatchers(HttpMethod.POST, "/api/v1/user/register-client").permitAll();
                req.requestMatchers(HttpMethod.GET, "/api/v1/user/approve").permitAll();
                req.requestMatchers(HttpMethod.DELETE, "/api/v1/user/delete").permitAll();
                req.requestMatchers(HttpMethod.POST, "/api/v1/user/updateData").hasRole("MANAGER");
                req.requestMatchers(HttpMethod.GET, "/api/v1/user").hasRole("MANAGER") ;
                req.requestMatchers(HttpMethod.POST, "/api/v1/user/register-user").hasRole("MANAGER");
                req.requestMatchers(HttpMethod.POST, "/api/v1/company/confirm").permitAll();
                req.requestMatchers(HttpMethod.POST, "/api/v1/company/save").hasRole("ADMIN");
                req.anyRequest().authenticated();
            }).addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                    .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
