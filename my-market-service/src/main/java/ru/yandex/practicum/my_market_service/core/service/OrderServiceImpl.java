package ru.yandex.practicum.my_market_service.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.api.handler.OrderItemNotFoundException;
import ru.yandex.practicum.my_market_service.core.mapper.OrderMapper;
import ru.yandex.practicum.my_market_service.core.model.OrderDto;
import ru.yandex.practicum.my_market_service.persistence.entity.Order;
import ru.yandex.practicum.my_market_service.persistence.entity.OrderItem;
import ru.yandex.practicum.my_market_service.persistence.model.OrderStatus;
import ru.yandex.practicum.my_market_service.persistence.repository.CartItemRepository;
import ru.yandex.practicum.my_market_service.persistence.repository.ItemRepository;
import ru.yandex.practicum.my_market_service.persistence.repository.OrderItemRepository;
import ru.yandex.practicum.my_market_service.persistence.repository.OrderRepository;
import yandex.practicum.market.client.api.PaymentApi;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final PaymentApi paymentApi;


    @Override
    public Mono<Order> createOrderFromCart(Long cartId) {
        return cartItemRepository.findByCartId(cartId)
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.error(new OrderItemNotFoundException("В корзине нет товаров для оформления заказа"));
                    }

                    Order order = new Order();
                    order.setOrderNumber(generateOrderNumber());
                    order.setStatus(OrderStatus.NEW);
                    order.setCreatedAt(LocalDateTime.now());
                    order.setUpdatedAt(LocalDateTime.now());
                    order.setTotalSum(0L);

                    return orderRepository.save(order)
                            .flatMap(savedOrder ->
                                    Flux.fromIterable(cartItems)
                                            .flatMap(cartItem ->
                                                    itemRepository.findById(cartItem.getItemId())
                                                            .map(item -> {
                                                                OrderItem orderItem = new OrderItem();
                                                                orderItem.setOrderId(savedOrder.getId());
                                                                orderItem.setItemId(cartItem.getItemId());
                                                                orderItem.setTitle(item.getTitle());
                                                                orderItem.setPrice(item.getPrice());
                                                                orderItem.setQuantity(cartItem.getQuantity());
                                                                return orderItem;
                                                            })
                                            )
                                            .collectList()
                                            .flatMap(orderItems -> {
                                                long totalSum = orderItems.stream()
                                                        .mapToLong(oi -> oi.getPrice() * oi.getQuantity())
                                                        .sum();

                                                savedOrder.setTotalSum(totalSum);

                                                List<OrderItem> sortedOrderItems = orderItems.stream()
                                                        .sorted(Comparator.comparing(OrderItem::getTitle))
                                                        .collect(Collectors.toList());

                                                return orderItemRepository.saveAll(sortedOrderItems)
                                                        .then(orderRepository.save(savedOrder))
                                                        .flatMap(updatedOrder ->
                                                                cartItemRepository.deleteByCartId(cartId)
                                                                        .thenReturn(updatedOrder)
                                                        );
                                            })
                            );
                });
    }

    @Override
    public Flux<OrderDto> getAllOrders() {
        return orderRepository.findAllOrderByCreatedAtDesc()
                .collectList()
                .flatMapMany(orders -> Flux.fromIterable(orders)
                        .flatMap(order -> orderItemRepository.findOrderItemByOrderId(order.getId())
                                .collectList()
                                .doOnNext(order::setOrderItems)
                                .thenReturn(order)
                        )
                )
                .map(orderMapper::convertToOrderDto);
    }

    @Override
    public Mono<OrderDto> getOrderById(Long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Заказ не найден: " + id)))
                .flatMap(order -> orderItemRepository.findOrderItemByOrderId(order.getId())
                        .collectList()
                        .doOnNext(order::setOrderItems)
                        .thenReturn(order))
                .map(orderMapper::convertToOrderDto);
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%05d", (int) (Math.random() * 100000));
        return "ORDER-" + timestamp + "-" + random;
    }
}