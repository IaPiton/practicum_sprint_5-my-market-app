package ru.yandex.practicum.my_market_app.core.service;

import java.util.Map;

public interface CartService {
    Long getCurrentCartId();

    Map<Long, Integer> getItemCounts(Long cartId);
}
