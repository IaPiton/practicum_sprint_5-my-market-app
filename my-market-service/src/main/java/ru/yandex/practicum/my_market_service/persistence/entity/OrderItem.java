package ru.yandex.practicum.my_market_service.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_items")
public class OrderItem {

    @Id
    private Long id;

    @Column(value = "order_id")
    private Long orderId;

    @Column(value = "item_id")
    private Long itemId;

    @Column(value = "title")
    private String title;

    @Column(value = "price")
    private Long price;

    @Column(value = "quantity")
    private Integer quantity;
}