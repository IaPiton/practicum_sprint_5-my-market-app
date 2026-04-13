package ru.yandex.practicum.my_market_app.core.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_app.core.model.ItemDto;
import ru.yandex.practicum.my_market_app.core.model.ItemsPageData;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;


public interface ItemService {
    Mono<ItemsPageData> getItemsPage(String search, String sort, int pageNumber, int pageSize, String sessionId);

    Mono<ItemDto> getItemById(Long id, String sessionId);

    Mono<String> updateCartItemAndGetRedirectUrl(Long id, String search, String sort, int pageNumber, int pageSize, String action, String sessionId);

    Mono<Item> getItemEntityById(Long itemId);

    Mono<ItemDto> updateItemCountAndGetItem(Long id, String action, String sessionId);
}