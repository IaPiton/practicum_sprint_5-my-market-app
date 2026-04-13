package ru.yandex.practicum.my_market_app.persistence.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.my_market_app.persistence.entity.OrderItem;

@Repository
public interface OrderItemRepository extends R2dbcRepository<OrderItem, Long> {
    @Query("""
            SELECT * FROM order_items oi join orders o on o.id = oi.order_id
            WHERE oi.order_id = :orderId
            order by o.created_at
            """)
    Flux<OrderItem> findOrderItemByOrderId(@Param("orderId")Long orderId);
}
