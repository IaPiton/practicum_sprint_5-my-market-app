package ru.yandex.practicum.my_market_app.core.service;

import ru.yandex.practicum.my_market_app.core.model.OrderDto;
import ru.yandex.practicum.my_market_app.persistence.entity.Order;

import java.util.List;

public interface OrderService {
    List<OrderDto> getAllOrders();

    Order createOrderFromCart(Long cartId);

    OrderDto getOrderById(Long id);
}