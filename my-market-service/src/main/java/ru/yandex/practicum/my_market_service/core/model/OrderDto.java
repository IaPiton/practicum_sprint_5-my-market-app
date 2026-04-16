package ru.yandex.practicum.my_market_service.core.model;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record OrderDto(Long id, String orderNumber, List<OrderItemDto> items, long totalSum, String status, LocalDateTime createdAt) {
}