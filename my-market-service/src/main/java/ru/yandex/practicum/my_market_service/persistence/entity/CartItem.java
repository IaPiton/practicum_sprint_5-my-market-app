package ru.yandex.practicum.my_market_service.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cart_items")
public class CartItem {

    @Id
    private Long id;

    @Column(value = "cart_id")
    private Long cartId;

    @Column(value = "item_id")
    private Long itemId;

    @Column(value = "quantity")
    private Integer quantity = 1;

    @Column(value = "added_at")
    private LocalDateTime addedAt;

    @Column(value = "updated_at")
    private LocalDateTime updatedAt;
}