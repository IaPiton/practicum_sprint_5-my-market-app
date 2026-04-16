package ru.yandex.practicum.payment_service.core.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.api.model.BalanceResponse;
import ru.yandex.practicum.payment.api.model.PaymentRequest;

import java.math.BigDecimal;

public interface PaymentService {
   BalanceResponse getBalance();

    Mono<BigDecimal> makePayment(Mono<PaymentRequest> paymentRequest);
}
