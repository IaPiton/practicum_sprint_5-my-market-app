package ru.yandex.practicum.my_market_service.core.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.core.model.CartItemDto;
import ru.yandex.practicum.my_market_service.persistence.entity.Item;

import java.math.BigDecimal;
import java.util.Map;

public interface CartService {
    Mono<Long> getCurrentCartId();

    Mono<Map<Long, Integer>> getItemCounts(Long cartId);

    Mono<Void> updateItemCount(Long cartId, Item item, String action);

    Flux<CartItemDto> getCartItemsWithDetails(Long cartId);

    Mono<Long> getCartTotal(Long cartId);

    Mono<BigDecimal> getBalance();
}
