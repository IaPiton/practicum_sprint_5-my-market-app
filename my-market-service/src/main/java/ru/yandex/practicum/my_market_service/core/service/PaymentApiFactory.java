package ru.yandex.practicum.my_market_service.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import yandex.practicum.market.client.ApiClient;
import yandex.practicum.market.client.api.PaymentApi;

@Component
public class PaymentApiFactory {

    private final WebClient.Builder webClientBuilder;
    private final String basePath;

    public PaymentApiFactory(WebClient.Builder webClientBuilder,
                             @Value("${PAYMENT_SERVICE_HOST:localhost}") String restHost,
                             @Value("${PAYMENT_SERVICE_PORT:8081}") int restPort) {
        this.webClientBuilder = webClientBuilder;
        this.basePath = "http://" + restHost + ":" + restPort;
    }

    public PaymentApi createWithToken(String accessToken) {
        WebClient webClient = webClientBuilder
                .clone()
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();

        ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(basePath);

        return new PaymentApi(apiClient);
    }
}