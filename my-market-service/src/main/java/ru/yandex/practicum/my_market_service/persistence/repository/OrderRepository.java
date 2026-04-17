package ru.yandex.practicum.my_market_service.persistence.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.my_market_service.persistence.entity.Order;


@Repository
public interface OrderRepository extends R2dbcRepository<Order, Long> {
    @Query("SELECT * FROM orders ORDER BY created_at DESC")
    Flux<Order> findAllOrderByCreatedAtDesc();
}