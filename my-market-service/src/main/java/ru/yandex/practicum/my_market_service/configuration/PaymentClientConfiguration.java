package ru.yandex.practicum.my_market_service.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yandex.practicum.market.client.ApiClient;
import yandex.practicum.market.client.api.PaymentApi;

@Configuration
public class PaymentClientConfiguration {
    @Bean
    public PaymentApi paymentApi(@Value("${PAYMENT_SERVICE_HOST:localhost}") String restHost, @Value("${PAYMENT_SERVICE_PORT:8081}") int restPort) {
        return new PaymentApi(new ApiClient().setBasePath("http://" + restHost + ":" + restPort));
    }
}
