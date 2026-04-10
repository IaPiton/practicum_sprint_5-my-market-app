package ru.yandex.practicum.my_market_app.api.handler;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(String message) {
        super(message);
    }
}
