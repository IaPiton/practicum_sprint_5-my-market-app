package ru.yandex.practicum.my_market_service.persistence.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.persistence.entity.Cart;

@Repository
public interface CartRepository extends R2dbcRepository<Cart, Long> {
    Mono<Cart> findByUserId(Long userId);
}
