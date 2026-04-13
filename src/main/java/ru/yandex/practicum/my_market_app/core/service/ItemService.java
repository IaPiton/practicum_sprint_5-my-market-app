package ru.yandex.practicum.my_market_app.core.service;

import ru.yandex.practicum.my_market_app.core.model.ItemsPageData;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;
import ru.yandex.practicum.my_market_app.core.model.ItemDto;

public interface ItemService {
    ItemsPageData getItemsPage(String search, String sort, int pageNumber, int pageSize, String sessionId);

    ItemDto getItemById(Long id, String sessionId);

    Item getItemEntityById(Long itemId);

    String updateCartItemAndGetRedirectUrl(Long id, String search, String sort,
                                           int pageNumber, int pageSize, String action, String sessionId);

    ItemDto updateItemCountAndGetItem(Long id, String action, String sessionId);
}
