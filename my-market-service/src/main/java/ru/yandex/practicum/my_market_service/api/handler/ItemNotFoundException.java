package ru.yandex.practicum.my_market_service.api.handler;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(String message) {
        super(message);
    }
}
