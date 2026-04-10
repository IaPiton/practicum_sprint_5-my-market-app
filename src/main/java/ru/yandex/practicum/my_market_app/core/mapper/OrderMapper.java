package ru.yandex.practicum.my_market_app.core.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.my_market_app.core.model.OrderDto;
import ru.yandex.practicum.my_market_app.core.model.OrderItemDto;
import ru.yandex.practicum.my_market_app.persistence.entity.Order;
import ru.yandex.practicum.my_market_app.persistence.entity.OrderItem;

import java.util.List;

@Component
public class OrderMapper {

    public OrderDto convertToOrderDto(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemDto> itemDtos = convertToOrderItemDtoList(order.getItems());
        long totalSum = itemDtos.stream()
                .mapToLong(OrderItemDto::subtotal)
                .sum();

        return OrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .items(itemDtos)
                .totalSum(totalSum)
                .status(order.getStatus() != null ? order.getStatus().name() : "NEW")
                .createdAt(order.getCreatedAt())
                .build();
    }

    public List<OrderDto> convertToOrderDtoList(List<Order> orders) {
        return orders.stream()
                .map(this::convertToOrderDto)
                .toList();
    }

    public OrderItemDto convertToOrderItemDto(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }

        long subtotal = orderItem.getPrice() * orderItem.getQuantity();

        return OrderItemDto.builder()
                .id(orderItem.getItem().getId())
                .title(orderItem.getTitle())
                .price(orderItem.getPrice())
                .count(orderItem.getQuantity())
                .subtotal(subtotal)
                .build();
    }

    public List<OrderItemDto> convertToOrderItemDtoList(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(this::convertToOrderItemDto)
                .toList();
    }
}