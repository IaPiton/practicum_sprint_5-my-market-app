package ru.yandex.practicum.my_market_service.persistence.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.persistence.entity.Item;

@Repository
public interface ItemRepository extends R2dbcRepository<Item, Long> {

    @Query(value = """
    SELECT * FROM items
    WHERE (cast(:search as text) IS NULL OR
           title ILIKE '%' || :search || '%' OR
           description ILIKE '%' || :search || '%')
    ORDER BY
        CASE WHEN :sortColumn = 'title' THEN title END ASC,
        CASE WHEN :sortColumn = 'price' THEN price END ASC,
        title ASC
    LIMIT :limit OFFSET :offset
    """)
    Flux<Item> searchAllItems(@Param("search") String search,
                              @Param("sortColumn") String sortColumn,
                              @Param("limit") int limit,
                              @Param("offset") long offset);

    @Query("""
            SELECT COUNT(*) FROM items WHERE (cast(:search as text) IS NULL OR
            title ILIKE '%' || :search || '%' OR
            description ILIKE '%' || :search || '%')
            """)
    Mono<Long> countBySearch(@Param("search") String search);
}
