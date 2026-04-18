package ru.yandex.practicum.my_market_service.configuration;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET, "/", "/items", "/images/*", "/items/*", "/login").permitAll()
                        .anyExchange().authenticated()
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .build();
    }

    @Bean
    @Primary
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder().encode("password"))
                .roles("USER")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin"))
                .roles("ADMIN")
                .build();

        return new MapReactiveUserDetailsService(user, admin);
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Моки для OAuth2
    @Bean
    @Primary
    public ReactiveOAuth2AuthorizedClientManager mockAuthorizedClientManager() {
        ReactiveOAuth2AuthorizedClientManager mock = Mockito.mock(ReactiveOAuth2AuthorizedClientManager.class);
        Mockito.when(mock.authorize(Mockito.any()))
                .thenReturn(Mono.just(Mockito.mock(org.springframework.security.oauth2.client.OAuth2AuthorizedClient.class)));
        return mock;
    }

    @Bean
    @Primary
    public ReactiveClientRegistrationRepository mockClientRegistrationRepository() {
        ReactiveClientRegistrationRepository mock = Mockito.mock(ReactiveClientRegistrationRepository.class);
        Mockito.when(mock.findByRegistrationId(Mockito.anyString()))
                .thenReturn(Mono.empty());
        return mock;
    }

    @Bean
    @Primary
    public ServerOAuth2AuthorizedClientRepository mockAuthorizedClientRepository() {
        return Mockito.mock(ServerOAuth2AuthorizedClientRepository.class);
    }
}