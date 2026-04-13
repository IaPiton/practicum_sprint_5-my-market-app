package ru.yandex.practicum.my_market_app.persistence.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;

@Repository
public interface ItemRepository extends R2dbcRepository<Item, Long> {

    @Query("SELECT * FROM items OFFSET :offset LIMIT :limit")
    Flux<Item> findAllNoSort(@Param("offset") long offset, @Param("limit") int limit);

    @Query("SELECT * FROM items ORDER BY title ASC OFFSET :offset LIMIT :limit")
    Flux<Item> findAllByTitleAsc(@Param("offset") long offset, @Param("limit") int limit);

    @Query("SELECT * FROM items ORDER BY title DESC OFFSET :offset LIMIT :limit")
    Flux<Item> findAllByTitleDesc(@Param("offset") long offset, @Param("limit") int limit);

    @Query("SELECT * FROM items ORDER BY price ASC OFFSET :offset LIMIT :limit")
    Flux<Item> findAllByPriceAsc(@Param("offset") long offset, @Param("limit") int limit);

    @Query("SELECT * FROM items ORDER BY price DESC OFFSET :offset LIMIT :limit")
    Flux<Item> findAllByPriceDesc(@Param("offset") long offset, @Param("limit") int limit);

    @Query("SELECT * FROM items WHERE LOWER(title) LIKE LOWER(:search) OFFSET :offset LIMIT :limit")
    Flux<Item> searchByTitleNoSort(@Param("search") String search,
                                   @Param("offset") long offset,
                                   @Param("limit") int limit);

    @Query("SELECT * FROM items WHERE LOWER(title) LIKE LOWER(:search) ORDER BY title ASC OFFSET :offset LIMIT :limit")
    Flux<Item> searchByTitleAsc(@Param("search") String search,
                                @Param("offset") long offset,
                                @Param("limit") int limit);

    @Query("SELECT * FROM items WHERE LOWER(title) LIKE LOWER(:search) ORDER BY title DESC OFFSET :offset LIMIT :limit")
    Flux<Item> searchByTitleDesc(@Param("search") String search,
                                 @Param("offset") long offset,
                                 @Param("limit") int limit);

    @Query("SELECT * FROM items WHERE LOWER(title) LIKE LOWER(:search) ORDER BY price ASC OFFSET :offset LIMIT :limit")
    Flux<Item> searchByPriceAsc(@Param("search") String search,
                                @Param("offset") long offset,
                                @Param("limit") int limit);

    @Query("SELECT * FROM items WHERE LOWER(title) LIKE LOWER(:search) ORDER BY price DESC OFFSET :offset LIMIT :limit")
    Flux<Item> searchByPriceDesc(@Param("search") String search,
                                 @Param("offset") long offset,
                                 @Param("limit") int limit);

    @Query("SELECT COUNT(*) FROM items WHERE LOWER(title) LIKE LOWER(:search)")
    Mono<Long> countBySearch(@Param("search") String search);
}
