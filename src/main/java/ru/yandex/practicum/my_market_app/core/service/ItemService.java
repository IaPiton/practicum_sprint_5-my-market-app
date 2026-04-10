package ru.yandex.practicum.my_market_app.core.service;

import ru.yandex.practicum.my_market_app.core.model.ItemsPageData;

public interface ItemService {
    ItemsPageData getItemsPage(String search, String sort, int pageNumber, int pageSize);
}
