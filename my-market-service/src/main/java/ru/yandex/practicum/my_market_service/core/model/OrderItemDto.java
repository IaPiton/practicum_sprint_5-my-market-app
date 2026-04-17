package ru.yandex.practicum.my_market_service.core.model;

import lombok.Builder;

@Builder
public record OrderItemDto(Long id, String title, Long price, int count, long subtotal) {
}