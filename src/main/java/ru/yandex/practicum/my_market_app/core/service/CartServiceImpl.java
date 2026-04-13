package ru.yandex.practicum.my_market_app.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_app.core.mapper.CartMapper;
import ru.yandex.practicum.my_market_app.core.model.CartItemDto;
import ru.yandex.practicum.my_market_app.persistence.entity.Cart;
import ru.yandex.practicum.my_market_app.persistence.entity.CartItem;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;
import ru.yandex.practicum.my_market_app.persistence.repository.CartItemRepository;
import ru.yandex.practicum.my_market_app.persistence.repository.CartRepository;
import ru.yandex.practicum.my_market_app.persistence.repository.ItemRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final CartRepository cartRepository;

    @Override
    public Mono<Long> getCurrentCartId(String sessionId) {
        return cartRepository.findBySessionId(sessionId)
                .switchIfEmpty(Mono.defer(() -> {
                    Cart newCart = new Cart();
                    newCart.setSessionId(sessionId);
                    return cartRepository.save(newCart);
                }))
                .map(Cart::getId);
    }

    @Override
    public Mono<Map<Long, Integer>> getItemCounts(Long cartId) {
        return cartItemRepository.findAllByCartId(cartId)
                .collectMap(
                        CartItem::getItemId,
                        CartItem::getQuantity
                );
    }

    @Override
    @Transactional
    public Mono<Void> updateItemCount(Long cartId, Item item, String action) {
        Mono<CartItem> cartItemMono = cartItemRepository.findByCartIdAndItemId(cartId, item.getId())
                .defaultIfEmpty(new CartItem(null, cartId, item.getId(), 0, LocalDateTime.now(), LocalDateTime.now()));

        return cartItemMono
                .flatMap(cartItem -> switch (action.toUpperCase()) {
                    case "PLUS" -> handlePlusAction(cartItem);
                    case "MINUS" -> handleMinusAction(cartItem);
                    case "DELETE" -> handleDeleteAction(cartItem);
                    default -> Mono.error(new IllegalArgumentException(
                            "Неизвестное действие: " + action + ". Поддерживаемые действия: PLUS, MINUS, DELETE"
                    ));
                });
    }

    @Override
    public Flux<CartItemDto> getCartItemsWithDetails(Long cartId) {
        return cartItemRepository.findByCartId(cartId)
                .flatMap(cartItem ->
                        itemRepository.findById(cartItem.getItemId())
                                .map(item -> cartMapper.convertToCartItemDto(cartItem, item))
                )
                .sort(Comparator.comparing(CartItemDto::title).reversed());
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<Long> getCartTotal(Long cartId) {
        return cartItemRepository.findByCartId(cartId)
                .flatMap(cartItem ->
                        itemRepository.findById(cartItem.getItemId())
                                .map(item -> item.getPrice() * cartItem.getQuantity())
                )
                .collectList()
                .map(list -> list.stream().mapToLong(Long::longValue).sum())
                .defaultIfEmpty(0L);
    }

    private Mono<Void> handlePlusAction(CartItem cartItem) {
        if (cartItem.getId() == null && cartItem.getQuantity() == 0) {
            cartItem.setQuantity(1);
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + 1);
        }
        cartItem.setUpdatedAt(LocalDateTime.now());
        return cartItemRepository.save(cartItem).then();
    }

    private Mono<Void> handleMinusAction(CartItem cartItem) {
        if (cartItem.getId() == null && cartItem.getQuantity() == 0) {
            return Mono.empty();
        }

        if (cartItem.getQuantity() > 1) {
            cartItem.setQuantity(cartItem.getQuantity() - 1);
            cartItem.setUpdatedAt(LocalDateTime.now());
            return cartItemRepository.save(cartItem).then();
        } else {
            return cartItemRepository.delete(cartItem).then();
        }
    }

    private Mono<Void> handleDeleteAction(CartItem cartItem) {
        if (cartItem.getId() != null) {
            return cartItemRepository.delete(cartItem).then();
        }
        return Mono.empty();
    }

}