package ru.yandex.practicum.my_market_service.core.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.core.model.OrderDto;
import ru.yandex.practicum.my_market_service.persistence.entity.Order;

public interface OrderService {
    Flux<OrderDto> getAllOrders(Long userId);

    Mono<Order> createOrderFromCart(Long cartId);

    Mono<OrderDto> getOrderById(Long id);
}