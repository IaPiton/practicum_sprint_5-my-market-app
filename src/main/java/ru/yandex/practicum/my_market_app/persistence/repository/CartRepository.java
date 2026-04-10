package ru.yandex.practicum.my_market_app.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.my_market_app.persistence.entity.Cart;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findBySessionId(String sessionId);
}
