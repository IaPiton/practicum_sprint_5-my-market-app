package ru.yandex.practicum.my_market_app.core.service;

import ru.yandex.practicum.my_market_app.core.model.CartItemDto;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;

import java.util.List;
import java.util.Map;

public interface CartService {
    Long getCurrentCartId();

    Map<Long, Integer> getItemCounts(Long cartId);

    void updateItemCount(Long cartId, Item item, String action);

    List<CartItemDto> getCartItemsWithDetails(Long cartId);

    Long getCartTotal(Long cartId);
}
