package com.neusoft.aihospital.filters;

import com.neusoft.aihospital.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpHeaders;

import java.util.List;

@Slf4j
@Component
public class AuthorizeFilter implements GlobalFilter, Ordered {
    private static final List<String> WHITE_LIST = List.of(
            "/auth/login",
            "/auth/login/email-code",
            "/auth/register",
            "/auth/email/send-code"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        if (WHITE_LIST.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String token = null;
        if (exchange.getRequest().getCookies().get("token") != null) {
            token = exchange.getRequest().getCookies().get("token").get(0).getValue();
        }

        if (token == null) {
            token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
        }

        if (token == null) {
            return unauthorized(exchange, "Token Missing");
        }

        try {
            Claims claims = JwtUtils.parseToken(token);

            exchange = exchange.mutate()
                    .request(builder -> builder.header("username", claims.getSubject()))
                    .build();

            return chain.filter(exchange);

        } catch (Exception e) {
            log.error("Token invalid: {}", e.getMessage());
            return unauthorized(exchange, "Token Invalid");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // 优先级高
    }
}
