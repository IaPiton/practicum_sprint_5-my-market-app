package ru.yandex.practicum.my_market_app.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.my_market_app.api.handler.CartNotFoundException;
import ru.yandex.practicum.my_market_app.core.mapper.CartMapper;
import ru.yandex.practicum.my_market_app.core.model.CartItemDto;
import ru.yandex.practicum.my_market_app.persistence.entity.Cart;
import ru.yandex.practicum.my_market_app.persistence.entity.CartItem;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;
import ru.yandex.practicum.my_market_app.persistence.repository.CartItemRepository;
import ru.yandex.practicum.my_market_app.persistence.repository.CartRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    @Override
    @Transactional
    public Long getCurrentCartId(String sessionId) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setSessionId(sessionId);
                    return cartRepository.save(newCart);
                });
        return cart.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getItemCounts(Long cartId) {
        Map<Long, Integer> counts = new HashMap<>();
        cartItemRepository.findByCartId(cartId).forEach(cartItem ->
                counts.put(cartItem.getItem().getId(), cartItem.getQuantity()));
        return counts;
    }

    @Override
    @Transactional
    public void updateItemCount(Long cartId, Item item, String action) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException("Корзина не найдена: " + cartId));

        CartItem cartItem = cartItemRepository.findByCartIdAndItemId(cartId, item.getId())
                .orElse(null);

         switch (action.toUpperCase()) {
            case "PLUS" -> handlePlusAction(cart, item, cartItem);
            case "MINUS" -> handleMinusAction(cartItem);
            case "DELETE" -> handleDeleteAction(cartItem);
            default -> throw new IllegalArgumentException(
                    "Неизвестное действие: " + action + ". Поддерживаемые действия: PLUS, MINUS, DELETE"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemDto> getCartItemsWithDetails(Long cartId) {
        List<CartItem> cartItems = cartItemRepository.findByCartId(cartId);

        return cartItems.stream()
                .map(cartMapper::convertToCartItemDto)
                .sorted(Comparator.comparing(CartItemDto::title).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getCartTotal(Long cartId) {
        return cartItemRepository.findByCartId(cartId).stream()
                .mapToLong(cartItem -> cartItem.getItem().getPrice() * cartItem.getQuantity())
                .sum();
    }

    private void handlePlusAction(Cart cart, Item item, CartItem cartItem) {
        if (cartItem == null) {
            CartItem newCartItem = new CartItem();
            newCartItem.setCart(cart);
            newCartItem.setItem(item);
            newCartItem.setQuantity(1);
            cartItemRepository.save(newCartItem);
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + 1);
            cartItemRepository.save(cartItem);
        }
    }

    private void handleMinusAction(CartItem cartItem) {
        if (cartItem == null) {
            return;
        }

        if (cartItem.getQuantity() > 1) {
            cartItem.setQuantity(cartItem.getQuantity() - 1);
            cartItemRepository.save(cartItem);
        } else {
            cartItemRepository.delete(cartItem);
        }
    }

    private void handleDeleteAction(CartItem cartItem) {
        if (cartItem != null) {
            cartItemRepository.delete(cartItem);
        }
    }

}