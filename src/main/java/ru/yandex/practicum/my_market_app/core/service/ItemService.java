package ru.yandex.practicum.my_market_app.core.service;

import ru.yandex.practicum.my_market_app.core.model.ItemsPageData;
import ru.yandex.practicum.my_market_app.persistence.model.ItemDto;

public interface ItemService {
    ItemsPageData getItemsPage(String search, String sort, int pageNumber, int pageSize);

    ItemDto getItemById(Long id);

    String updateCartItemAndGetRedirectUrl(Long id, String search, String sort,
                                           int pageNumber, int pageSize, String action);

    ItemDto updateItemCountAndGetItem(Long id, String action);
}
