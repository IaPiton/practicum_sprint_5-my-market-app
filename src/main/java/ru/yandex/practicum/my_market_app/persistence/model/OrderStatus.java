package ru.yandex.practicum.my_market_app.persistence.model;

public enum OrderStatus {
    NEW("Новый"),
    PAID("Оплачен"),
    SHIPPED("Отправлен"),
    DELIVERED("Доставлен"),
    CANCELLED("Отменен");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
