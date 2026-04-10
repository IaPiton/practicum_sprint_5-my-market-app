package ru.yandex.practicum.my_market_app.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.my_market_app.persistence.entity.CartItem;

import java.util.List;


@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartId(Long cartId);
}
