package ru.yandex.practicum.my_market_app.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
}
