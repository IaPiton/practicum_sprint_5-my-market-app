package ru.yandex.practicum.my_market_service.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.yandex.practicum.my_market_service.persistence.entity.CartItem;
import ru.yandex.practicum.my_market_service.persistence.entity.Item;

@Data
@AllArgsConstructor
public class OrderItemContext {
    private CartItem cartItem;
    private Item item;
}