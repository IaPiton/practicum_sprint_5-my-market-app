package ru.yandex.practicum.my_market_service.api.handler;

public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message) {
        super(message);
    }
}
