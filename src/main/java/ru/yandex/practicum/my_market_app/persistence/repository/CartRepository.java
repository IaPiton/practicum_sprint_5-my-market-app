package ru.yandex.practicum.my_market_app.persistence.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.my_market_app.persistence.entity.Cart;

import java.util.Optional;

@Repository
public interface CartRepository extends R2dbcRepository<Cart, Long> {

    Optional<Cart> findBySessionId(String sessionId);
}
