package ru.yandex.practicum.my_market_service.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    public Mono<String> getTokenValue() {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("keycloak")
                .principal("store-showcase")
                .build();

        return Mono.fromCallable(() -> authorizeRequest)
                .flatMap(authorizedClientManager::authorize)
                .map(OAuth2AuthorizedClient::getAccessToken)
                .map(OAuth2AccessToken::getTokenValue)
                .onErrorMap(ex -> new IllegalStateException("Не удалось получить токен OAuth2", ex));
    }
}
