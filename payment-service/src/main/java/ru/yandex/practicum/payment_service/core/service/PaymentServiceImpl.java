package ru.yandex.practicum.payment_service.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.api.model.BalanceResponse;
import ru.yandex.practicum.payment.api.model.PaymentRequest;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    @Value("${service.default-balance}")
    private BigDecimal defaultBalance;

    @Override
    public BalanceResponse getBalance() {
        return new BalanceResponse(defaultBalance);
    }

    @Override
    public Mono<Long> makePayment(Mono<PaymentRequest> paymentRequest) {
        return null;
    }
}
