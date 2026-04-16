package ru.yandex.practicum.my_market_service.api.handler;

public class CartNotFoundException extends RuntimeException {
    public CartNotFoundException(String message) {
        super(message);
    }
}