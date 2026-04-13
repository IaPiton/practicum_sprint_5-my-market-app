package ru.yandex.practicum.my_market_app.api.handler;

public class CartNotFoundException extends RuntimeException {
    public CartNotFoundException(String message) {
        super(message);
    }
}