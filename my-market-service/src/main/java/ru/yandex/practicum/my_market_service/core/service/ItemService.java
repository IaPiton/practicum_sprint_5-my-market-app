package ru.yandex.practicum.my_market_service.core.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.core.model.ItemDto;
import ru.yandex.practicum.my_market_service.core.model.ItemsPageData;


public interface ItemService {
    Mono<ItemsPageData> getItemsPage(String search, String sort, int pageNumber, int pageSize);

    Mono<ItemDto> getItemById(Long id);

    Mono<String> updateCartItemAndGetRedirectUrl(Long id, String search, String sort, int pageNumber, int pageSize, String action);

    Mono<ItemDto> updateItemCountAndGetItem(Long id, String action);
}