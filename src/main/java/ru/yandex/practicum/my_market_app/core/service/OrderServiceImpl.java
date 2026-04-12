package ru.yandex.practicum.my_market_app.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.my_market_app.api.handler.OrderItemNotFoundException;
import ru.yandex.practicum.my_market_app.core.mapper.OrderMapper;
import ru.yandex.practicum.my_market_app.core.model.OrderDto;
import ru.yandex.practicum.my_market_app.persistence.entity.CartItem;
import ru.yandex.practicum.my_market_app.persistence.entity.Order;
import ru.yandex.practicum.my_market_app.persistence.entity.OrderItem;
import ru.yandex.practicum.my_market_app.persistence.model.OrderStatus;
import ru.yandex.practicum.my_market_app.persistence.repository.CartItemRepository;
import ru.yandex.practicum.my_market_app.persistence.repository.OrderRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderMapper orderMapper;

    @Override
    public List<OrderDto> getAllOrders() {
        List<Order> orders = orderRepository.findAllOrderByCreatedAtDesc();
        return orders.stream()
                .map(orderMapper::convertToOrderDto)
                .toList();
    }

    @Override
    public Order createOrderFromCart(Long cartId) {
        List<CartItem> cartItems = cartItemRepository.findByCartId(cartId);

        if (cartItems.isEmpty()) {
            throw new OrderItemNotFoundException("В корзине нет товаров для оформления заказа");
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.NEW);

        long totalSum = 0L;

        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setItem(cartItem.getItem());
            orderItem.setTitle(cartItem.getItem().getTitle());
            orderItem.setPrice(cartItem.getItem().getPrice());
            orderItem.setQuantity(cartItem.getQuantity());

            order.getItems().add(orderItem);
            totalSum += cartItem.getItem().getPrice() * cartItem.getQuantity();
        }

        order.setTotalSum(totalSum);
        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteByCartId(cartId);
        return savedOrder;
    }


    @Override
    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + id));
        return orderMapper.convertToOrderDto(order);
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%05d", (int)(Math.random() * 100000));
        return "ORDER-" + timestamp + "-" + random;
    }
}