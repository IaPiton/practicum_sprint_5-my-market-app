package ru.yandex.practicum.payment_service.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.api.model.BalanceResponse;
import ru.yandex.practicum.payment.api.model.PaymentRequest;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService{
    @Override
    public BalanceResponse getBalance() {
        return null;
    }

    @Override
    public Mono<Long> makePayment(Mono<PaymentRequest> paymentRequest) {
        return null;
    }
}
