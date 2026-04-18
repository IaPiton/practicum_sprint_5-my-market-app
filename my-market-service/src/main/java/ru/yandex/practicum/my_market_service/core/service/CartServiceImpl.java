package ru.yandex.practicum.my_market_service.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.api.handler.CartNotFoundException;
import ru.yandex.practicum.my_market_service.core.mapper.CartMapper;
import ru.yandex.practicum.my_market_service.core.model.CartItemDto;
import ru.yandex.practicum.my_market_service.core.security.OAuth2Service;
import ru.yandex.practicum.my_market_service.core.security.SecurityService;
import ru.yandex.practicum.my_market_service.persistence.entity.Cart;
import ru.yandex.practicum.my_market_service.persistence.entity.CartItem;
import ru.yandex.practicum.my_market_service.persistence.entity.Item;
import ru.yandex.practicum.my_market_service.persistence.repository.CartItemRepository;
import ru.yandex.practicum.my_market_service.persistence.repository.CartRepository;
import ru.yandex.practicum.my_market_service.persistence.repository.ItemRepository;
import yandex.practicum.market.client.api.PaymentApi;
import yandex.practicum.market.client.model.BalanceResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final CartRepository cartRepository;
    private final PaymentApi paymentApi;
    private final SecurityService securityService;
    private final OAuth2Service oAuth2Service;

    @Override
    public Mono<Long> getCurrentCartId() {
        return securityService.getCurrentUserId()
                .flatMap(userId -> cartRepository.findByUserId(userId)
                        .switchIfEmpty(Mono.defer(() -> {
                            Cart newCart = new Cart();
                            newCart.setUserId(userId);
                            return cartRepository.save(newCart);
                        }))
                        .map(Cart::getId)
                );
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
        return cartRepository.findById(cartId)
                .switchIfEmpty(Mono.error(new CartNotFoundException("Корзина не найдена: " + cartId)))
                .flatMap(cart -> cartItemRepository.findByCartIdAndItemId(cartId, item.getId())
                        .defaultIfEmpty(new CartItem(null, cartId, item.getId(), 0,
                                LocalDateTime.now(), LocalDateTime.now()))
                        .flatMap(cartItem -> switch (action.toUpperCase()) {
                            case "PLUS" -> handlePlusAction(cart, cartItem);
                            case "MINUS" -> handleMinusAction(cart, cartItem);
                            case "DELETE" -> handleDeleteAction(cartItem);
                            default -> Mono.error(new IllegalArgumentException(
                                    "Неизвестное действие: " + action +
                                            ". Поддерживаемые действия: PLUS, MINUS, DELETE"
                            ));
                        }));
    }

    @Override
    public Flux<CartItemDto> getCartItemsWithDetails(Long cartId) {
        return cartItemRepository.findByCartId(cartId)
                .collectList()
                .flatMapMany(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Flux.empty();
                    }

                    List<Long> itemIds = cartItems.stream()
                            .map(CartItem::getItemId)
                            .distinct()
                            .collect(Collectors.toList());

                    return itemRepository.findAllById(itemIds)
                            .collectMap(Item::getId, Function.identity())
                            .flatMapMany(itemMap -> Flux.fromIterable(cartItems)
                                    .map(cartItem -> {
                                        Item item = itemMap.get(cartItem.getItemId());
                                        return cartMapper.convertToCartItemDto(cartItem, item);
                                    })
                                    .sort(Comparator.comparing(CartItemDto::title).reversed())
                            );
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<Long> getCartTotal(Long cartId) {
        return cartItemRepository.findByCartId(cartId)
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.just(0L);
                    }

                    List<Long> itemIds = cartItems.stream()
                            .map(CartItem::getItemId)
                            .distinct()
                            .collect(Collectors.toList());

                    return itemRepository.findAllById(itemIds)
                            .collectMap(Item::getId, Function.identity())
                            .map(itemMap -> cartItems.stream()
                                    .mapToLong(cartItem -> {
                                        Item item = itemMap.get(cartItem.getItemId());
                                        return item != null ? item.getPrice() * cartItem.getQuantity() : 0L;
                                    })
                                    .sum());
                });
    }

    @Override
    public Mono<BigDecimal> getBalance() {
        return oAuth2Service
                .getTokenValue()
                .flatMap(accessToken -> {
                    paymentApi.getApiClient().addDefaultHeader("Authorization", "Bearer " + accessToken);
                    return paymentApi.getBalance();
                })
                .map(BalanceResponse::getBalance)
                .onErrorResume(error -> Mono.just(BigDecimal.ONE.negate()));
    }

    private Mono<Void> handlePlusAction(Cart cart, CartItem cartItem) {
        if (cartItem.getId() == null) {
            cartItem.setQuantity(1);
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + 1);
        }
        cartItem.setUpdatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());

        return cartItemRepository.save(cartItem)
                .then(cartRepository.save(cart))
                .then();
    }

    private Mono<Void> handleMinusAction(Cart cart, CartItem cartItem) {
        if (cartItem.getId() == null) {
            return Mono.empty();
        }

        if (cartItem.getQuantity() > 1) {
            cartItem.setQuantity(cartItem.getQuantity() - 1);
            cartItem.setUpdatedAt(LocalDateTime.now());
            cart.setUpdatedAt(LocalDateTime.now());
            return cartItemRepository.save(cartItem)
                    .then(cartRepository.save(cart))
                    .then();
        } else {
            cart.setUpdatedAt(LocalDateTime.now());
            return cartItemRepository.delete(cartItem)
                    .then(cartRepository.save(cart))
                    .then();
        }
    }

    private Mono<Void> handleDeleteAction(CartItem cartItem) {
        if (cartItem.getId() != null) {
            return cartItemRepository.delete(cartItem).then();
        }
        return Mono.empty();
    }

}