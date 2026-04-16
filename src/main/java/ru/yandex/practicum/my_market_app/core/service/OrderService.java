package ru.yandex.practicum.my_market_app.core.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_app.core.model.OrderDto;
import ru.yandex.practicum.my_market_app.persistence.entity.Order;

public interface OrderService {
    Flux<OrderDto> getAllOrders();

    Mono<Order> createOrderFromCart(Long cartId);

    Mono<OrderDto> getOrderById(Long id);
}