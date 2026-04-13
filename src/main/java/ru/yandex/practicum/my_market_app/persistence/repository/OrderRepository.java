package ru.yandex.practicum.my_market_app.persistence.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.my_market_app.persistence.entity.Order;


@Repository
public interface OrderRepository extends R2dbcRepository<Order, Long> {
//    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
//    List<Order> findAllOrderByCreatedAtDesc();
}