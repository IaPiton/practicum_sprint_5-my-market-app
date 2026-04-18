package ru.yandex.practicum.my_market_service.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PaymentClientConfiguration {
    @Value("${PAYMENT_SERVICE_HOST:localhost}")
    private String restHost;

    @Value("${PAYMENT_SERVICE_PORT:8081}")
    private int restPort;

    private String getBasePath() {
        return "http://" + restHost + ":" + restPort;
    }

    @Bean
    public WebClient.Builder paymentWebClientBuilder() {
        return WebClient.builder()
                .baseUrl(getBasePath())
                .codecs(ClientCodecConfigurer::defaultCodecs);
    }

}

