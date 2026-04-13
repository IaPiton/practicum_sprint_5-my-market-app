package ru.yandex.practicum.my_market_app.persistence.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_app.persistence.entity.CartItem;


@Repository
public interface CartItemRepository extends R2dbcRepository<CartItem, Long> {
    Flux<CartItem> findAllByCartId(Long cartId);

    Mono<CartItem> findByCartIdAndItemId(Long cartId, Long id);

    Flux<CartItem> findByCartId(Long cartId);

    <T> Mono<T> deleteByCartId(Long cartId);

}