package ru.yandex.practicum.my_market_service.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.api.handler.OrderItemNotFoundException;
import ru.yandex.practicum.my_market_service.api.handler.PaymentFailedException;
import ru.yandex.practicum.my_market_service.core.mapper.OrderMapper;
import ru.yandex.practicum.my_market_service.core.model.OrderDto;
import ru.yandex.practicum.my_market_service.core.model.OrderItemContext;
import ru.yandex.practicum.my_market_service.core.model.OrderTotalContext;
import ru.yandex.practicum.my_market_service.persistence.entity.CartItem;
import ru.yandex.practicum.my_market_service.persistence.entity.Order;
import ru.yandex.practicum.my_market_service.persistence.entity.OrderItem;
import ru.yandex.practicum.my_market_service.persistence.model.OrderStatus;
import ru.yandex.practicum.my_market_service.persistence.repository.CartItemRepository;
import ru.yandex.practicum.my_market_service.persistence.repository.ItemRepository;
import ru.yandex.practicum.my_market_service.persistence.repository.OrderItemRepository;
import ru.yandex.practicum.my_market_service.persistence.repository.OrderRepository;
import yandex.practicum.market.client.api.PaymentApi;
import yandex.practicum.market.client.model.PaymentRequest;

import java.math.BigDecimal;
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
                .filter(cartItems -> !cartItems.isEmpty())
                .switchIfEmpty(Mono.error(new OrderItemNotFoundException("В корзине нет товаров для оформления заказа")))
                .flatMap(this::calculateTotalSum)
                .flatMap(this::processPaymentAndCreateOrder);
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

    private Mono<OrderTotalContext> calculateTotalSum(List<CartItem> cartItems) {
        return Flux.fromIterable(cartItems)
                .flatMap(cartItem -> itemRepository.findById(cartItem.getItemId())
                        .map(item -> new OrderItemContext(cartItem, item)))
                .collectList()
                .map(contexts -> {
                    long totalSum = contexts.stream()
                            .mapToLong(ctx -> ctx.getItem().getPrice() * ctx.getCartItem().getQuantity())
                            .sum();
                    return new OrderTotalContext(cartItems, contexts, totalSum);
                });
    }

    private Mono<Order> processPaymentAndCreateOrder(OrderTotalContext context) {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setSum(BigDecimal.valueOf(context.getTotalSum()));

        return paymentApi.makePayment(paymentRequest)
                .then(createOrder(context))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                        return Mono.error(new PaymentFailedException("Недостаточно средств на балансе"));
                    }
                    return Mono.error(new PaymentFailedException("Ошибка при обработке платежа"));
                })
                .onErrorResume(Exception.class, ex -> Mono.error(new PaymentFailedException("Ошибка при обработке платежа")));
    }

    private Mono<Order> createOrder(OrderTotalContext context) {
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.NEW);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setTotalSum(context.getTotalSum());

        return orderRepository.save(order)
                .flatMap(savedOrder -> saveOrderItems(savedOrder, context))
                .flatMap(updatedOrder -> cartItemRepository.deleteByCartId(context.getCartItems().get(0).getCartId())
                        .thenReturn(updatedOrder));
    }

    private Mono<Order> saveOrderItems(Order order, OrderTotalContext context) {
        List<OrderItem> orderItems = context.getContexts().stream()
                .map(ctx -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrderId(order.getId());
                    orderItem.setItemId(ctx.getItem().getId());
                    orderItem.setTitle(ctx.getItem().getTitle());
                    orderItem.setPrice(ctx.getItem().getPrice());
                    orderItem.setQuantity(ctx.getCartItem().getQuantity());
                    return orderItem;
                })
                .sorted(Comparator.comparing(OrderItem::getTitle))
                .collect(Collectors.toList());

        return orderItemRepository.saveAll(orderItems)
                .then(orderRepository.save(order));
    }


    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%05d", (int) (Math.random() * 100000));
        return "ORDER-" + timestamp + "-" + random;
    }
}