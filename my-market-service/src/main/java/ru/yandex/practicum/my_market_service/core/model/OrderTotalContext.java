package ru.yandex.practicum.my_market_service.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.yandex.practicum.my_market_service.persistence.entity.CartItem;

import java.util.List;

@Data
@AllArgsConstructor
public final class OrderTotalContext {
    private List<CartItem> cartItems;
    private List<OrderItemContext> contexts;
    private long totalSum;
}