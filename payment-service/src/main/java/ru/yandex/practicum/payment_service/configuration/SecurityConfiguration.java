package ru.yandex.practicum.payment_service.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

import java.util.List;

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(customizer -> customizer
                        .jwt(jwtCustomizer -> {
                            var jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
                            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
                                List<String> roles = jwt.getClaim("roles");
                                return Flux.fromIterable(roles)
                                        .map(SimpleGrantedAuthority::new);
                            });

                            jwtCustomizer.jwtAuthenticationConverter(jwtAuthenticationConverter);
                        })
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }
}
