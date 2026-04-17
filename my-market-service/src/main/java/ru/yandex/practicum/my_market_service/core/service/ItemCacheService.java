package ru.yandex.practicum.my_market_service.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.api.handler.ItemNotFoundException;
import ru.yandex.practicum.my_market_service.persistence.entity.Item;
import ru.yandex.practicum.my_market_service.persistence.repository.ItemRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemCacheService {
    private final ItemRepository itemRepository;

    @Cacheable(value = "allItems", key = "#search + '-' + #limit + '-' + #offset + '-' + #sort")
    public Mono<List<Item>> searchAllItems(String search, String sort, int limit, long offset) {
        return itemRepository.searchAllItems(search, sort, limit, offset)
                .collectList();
    }

    @Cacheable(value = "items", key = "#itemId", unless = "#result == null")
    public Mono<Item> getItemEntityById(Long itemId) {
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Товар не найден: " + itemId)));
    }
}
