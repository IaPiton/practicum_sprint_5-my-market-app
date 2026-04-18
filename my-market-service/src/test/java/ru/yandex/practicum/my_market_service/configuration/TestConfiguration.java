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
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.core.security.OAuth2Service;
import ru.yandex.practicum.my_market_service.core.security.SecurityService;
import ru.yandex.practicum.my_market_service.core.service.PaymentApiFactory;
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
    public PaymentApiFactory paymentApiFactory() {
        PaymentApi mockPaymentApi = Mockito.mock(PaymentApi.class);

        when(mockPaymentApi.makePayment(any(PaymentRequest.class)))
                .thenReturn(Mono.empty());

        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setBalance(BigDecimal.valueOf(1000));
        when(mockPaymentApi.getBalance())
                .thenReturn(Mono.just(balanceResponse));

        PaymentApiFactory mockFactory = Mockito.mock(PaymentApiFactory.class);
        when(mockFactory.createWithToken(any(String.class)))
                .thenReturn(mockPaymentApi);

        return mockFactory;
    }

    @Bean
    @Primary
    public SecurityService mockSecurityService () {
        SecurityService  mock = Mockito.mock(SecurityService .class);

        when(mock.getCurrentUserId())
                .thenReturn(Mono.just(3L));

        return mock;
    }

    @Bean
    @Primary
    public OAuth2Service mockOAuth2Service() {
        OAuth2Service mock = Mockito.mock(OAuth2Service.class);

        when(mock.getTokenValue())
                .thenReturn(Mono.just("test-access-token-12345"));

        return mock;
    }

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

