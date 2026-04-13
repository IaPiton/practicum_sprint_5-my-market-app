package ru.yandex.practicum.my_market_app.persistence.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;

@Repository
public interface ItemRepository extends R2dbcRepository<Item, Long> {

//    @Query("SELECT i FROM Item i WHERE " +
//            "(:search IS NULL OR :search = '' OR " +
//            "LOWER(i.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
//            "LOWER(i.description) LIKE LOWER(CONCAT('%', :search, '%')))")
//    Page<Item> searchItems(@Param("search") String search, Pageable pageable);
}
