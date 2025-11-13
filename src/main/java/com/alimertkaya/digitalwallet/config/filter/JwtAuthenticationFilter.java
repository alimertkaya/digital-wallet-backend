package com.alimertkaya.digitalwallet.config.filter;

import com.alimertkaya.digitalwallet.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component // bean oldugunu belirtir
@RequiredArgsConstructor
@Slf4j // logloma icin
public class JwtAuthenticationFilter implements WebFilter {

    public static final String HEADER_PREFIX = "Bearer ";

    public final JwtService jwtService;
    public final ReactiveUserDetailsService userDetailsService;

    // gelen her web istegini filtreler
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // auth basligini alir
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(HEADER_PREFIX)) {
            // sureci devam ettir
            return chain.filter(exchange);
        }

        // bearer kismi atilir
        String token = authHeader.substring(HEADER_PREFIX.length());

        String username = null;
        try {
            username = jwtService.extractUsername(token);
        } catch (Exception e) {
            log.warn("JWT Token okunamadı veya süresi dolmuş: {}", e.getMessage());
            return chain.filter(exchange);
        }

        if (username == null || username.isBlank()) {
            return chain.filter(exchange);
        }

        return userDetailsService.findByUsername(username)
                .flatMap(user -> {
                    // token db den gelen user icin gecerli mi
                    if (!jwtService.isTokenValid(token, user)) {
                            return chain.filter(exchange);
                    }
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
                    var context = new SecurityContextImpl(authentication);

                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));
                })
                // user bulunmazsa
                .onErrorResume(ex -> {
                    log.debug("userDeetailsService error: {}", ex.toString());
                    return chain.filter(exchange);
                });
    }
}