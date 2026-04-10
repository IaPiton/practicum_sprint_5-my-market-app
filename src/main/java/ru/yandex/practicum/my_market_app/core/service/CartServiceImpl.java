package ru.yandex.practicum.my_market_app.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.my_market_app.persistence.entity.Cart;
import ru.yandex.practicum.my_market_app.persistence.repository.CartItemRepository;
import ru.yandex.practicum.my_market_app.persistence.repository.CartRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional
    public Long getCurrentCartId() {
        String sessionId = getCurrentSessionId();
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


    private String getCurrentSessionId() {
        return "default-session";
    }
}
