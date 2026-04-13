package ru.yandex.practicum.my_market_app.core.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.my_market_app.core.model.CartItemDto;
import ru.yandex.practicum.my_market_app.persistence.entity.CartItem;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;

@Component
@RequiredArgsConstructor
public class CartMapper {

    public CartItemDto convertToCartItemDto(CartItem cartItem, Item item) {
        int quantity = cartItem.getQuantity();
        long subtotal = item.getPrice() * quantity;

        return CartItemDto.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .imgPath(item.getImgPath())
                .price(item.getPrice())
                .count(quantity)
                .subtotal(subtotal)
                .build();
    }
}