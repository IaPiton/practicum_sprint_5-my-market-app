package ru.yandex.practicum.my_market_app.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query("SELECT i FROM Item i WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(i.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(i.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Item> searchItems(@Param("search") String search, Pageable pageable);
}
