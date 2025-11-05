package com.alimertkaya.digitalwallet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity // metod bazli guvenlik
public class SecurityConfig {

    // password u hashlemek icin
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // guvenlik kurallari
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // API tabanli, stateless (JWT) tasarimi oldugu icin CSRF i devre disi
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);

        // HTTP basic ve form login devre disi kendimiz olusturacagiz
        http.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable);
        http.formLogin(ServerHttpSecurity.FormLoginSpec::disable);

        // URL bazli yetkilendirme
        http.authorizeExchange(exchanges -> exchanges
                // bu yollara izin ver
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                // kalan istekler icin authentication iste
                .anyExchange().authenticated()
        );

        // JWT filtresi gelecek

        return http.build();
    }

}
