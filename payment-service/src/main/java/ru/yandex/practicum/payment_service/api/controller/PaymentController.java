package ru.yandex.practicum.payment_service.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.api.controller.BalanceApi;
import ru.yandex.practicum.payment.api.model.BalanceResponse;
import ru.yandex.practicum.payment.api.model.PaymentRequest;
import ru.yandex.practicum.payment_service.core.service.PaymentService;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class PaymentController implements BalanceApi {
    private final PaymentService paymentService;

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getBalance(final ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(paymentService.getBalance()));
    }

    @Override
    public Mono<ResponseEntity<Void>> makePayment(
            Mono<PaymentRequest> paymentRequest,
            final ServerWebExchange exchange
    ) {
        return paymentService.makePayment(paymentRequest)
                .flatMap(result ->
                        result.compareTo(BigDecimal.ZERO) < 0
                        ? Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
                        : Mono.just(ResponseEntity.ok().build()));
    }

}