package ru.yandex.practicum.my_market_service.core.model;


import lombok.Builder;

@Builder
public record CartItemDto (Long id, String title, String description, String imgPath, Long price, int count, long subtotal) {

}
