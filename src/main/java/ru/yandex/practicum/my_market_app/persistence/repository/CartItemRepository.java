package ru.yandex.practicum.my_market_app.persistence.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.my_market_app.persistence.entity.CartItem;


@Repository
public interface CartItemRepository extends R2dbcRepository<CartItem, Long> {
//    List<CartItem> findByCartId(Long cartId);
//
//    Optional<CartItem> findByCartIdAndItemId(Long cartId, Long itemId);
//
//    @Modifying
//    @Transactional
//    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
//    void deleteByCartId(Long cartId);
}