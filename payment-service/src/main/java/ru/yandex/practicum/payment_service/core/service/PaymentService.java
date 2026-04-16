package ru.yandex.practicum.payment_service.core.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.api.model.BalanceResponse;
import ru.yandex.practicum.payment.api.model.PaymentRequest;

public interface PaymentService {
   BalanceResponse getBalance();

    Mono<Long> makePayment(Mono<PaymentRequest> paymentRequest);
}
