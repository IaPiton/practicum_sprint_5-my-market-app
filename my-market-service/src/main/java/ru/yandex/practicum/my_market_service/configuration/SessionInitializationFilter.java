package ru.yandex.practicum.my_market_service.configuration;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class SessionInitializationFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return exchange.getSession()
                .flatMap(session -> {
                    if (!session.getAttributes().containsKey("initialized")) {
                        session.getAttributes().put("initialized", true);
                        session.getAttributes().put("createdAt", System.currentTimeMillis());
                        return session.save().then(chain.filter(exchange));
                    }
                    return chain.filter(exchange);
                });
    }
}