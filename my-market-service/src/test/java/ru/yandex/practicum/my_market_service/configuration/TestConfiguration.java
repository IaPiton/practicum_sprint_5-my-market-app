package ru.yandex.practicum.my_market_service.configuration;


import io.r2dbc.spi.ConnectionFactory;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import reactor.core.publisher.Mono;
import yandex.practicum.market.client.api.PaymentApi;
import yandex.practicum.market.client.model.BalanceResponse;
import yandex.practicum.market.client.model.PaymentRequest;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@org.springframework.boot.test.context.TestConfiguration
public class TestConfiguration {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Bean
    public ConnectionFactoryInitializer databaseInitializer() {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
        populator.addPopulators(new ResourceDatabasePopulator(new ClassPathResource("init.sql")));
        initializer.setDatabasePopulator(populator);

        return initializer;
    }

    @Bean
    @Primary
    public PaymentApi mockPaymentApi() {
        PaymentApi mock = Mockito.mock(PaymentApi.class);

        when(mock.makePayment(any(PaymentRequest.class)))
                .thenReturn(Mono.empty());

        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setBalance(BigDecimal.valueOf(1000));

        when(mock.getBalance())
                .thenReturn(Mono.just(balanceResponse));
        return mock;
    }
}
