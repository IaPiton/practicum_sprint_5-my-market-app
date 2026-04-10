package ru.yandex.practicum.my_market_app.core.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.my_market_app.api.handler.ItemNotFoundException;
import ru.yandex.practicum.my_market_app.core.mapper.ItemMapper;
import ru.yandex.practicum.my_market_app.core.mapper.ItemsGridBuilder;
import ru.yandex.practicum.my_market_app.core.mapper.PageableBuilder;
import ru.yandex.practicum.my_market_app.core.model.ItemsPageData;
import ru.yandex.practicum.my_market_app.core.model.PagingInfo;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;
import ru.yandex.practicum.my_market_app.persistence.model.ItemDto;
import ru.yandex.practicum.my_market_app.persistence.repository.ItemRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final CartService cartService;
    private final ItemMapper itemMapper;
    private final ItemsGridBuilder gridBuilder;
    private final PageableBuilder pageableBuilder;

    @Override
    public ItemsPageData getItemsPage(String search, String sort, int pageNumber, int pageSize) {
        Long cartId = cartService.getCurrentCartId();

        Map<Long, Integer> cartItemCounts = cartService.getItemCounts(cartId);

        Pageable pageable = pageableBuilder.createPageable(pageNumber, pageSize, sort);

        Page<Item> itemPage = getItemsPageWithSearch(search, pageable);

        List<ItemDto> itemsWithCount = itemMapper.toDtoList(itemPage.getContent(), cartItemCounts);

        List<List<ItemDto>> itemsGrid = gridBuilder.buildGrid(itemsWithCount, 3);

        PagingInfo pagingInfo = new PagingInfo(
                pageNumber,
                pageSize,
                itemPage.hasPrevious(),
                itemPage.hasNext(),
                itemPage.getTotalPages(),
                itemPage.getTotalElements()
        );

        return new ItemsPageData(itemsGrid, search, sort, pagingInfo);
    }

    @Override
    public ItemDto getItemById(Long itemId) {
        Long cartId = cartService.getCurrentCartId();
        Item item = getItemEntityById(itemId);
        Map<Long, Integer> cartItemCounts = cartService.getItemCounts(cartId);
        int updatedCount = cartItemCounts.getOrDefault(item.getId(), 0);
        return itemMapper.toDto(item, updatedCount);
    }

    @Override
    @Transactional
    public String updateCartItemAndGetRedirectUrl(Long itemId, String search, String sort,
                                                  int pageNumber, int pageSize, String action) {
        Long cartId = cartService.getCurrentCartId();

        Item item = getItemEntityById(itemId);

        cartService.updateItemCount(cartId, item, action);

        return buildRedirectUrl(search, sort, pageNumber, pageSize);
    }

    @Override
    @Transactional
    public ItemDto updateItemCountAndGetItem(Long itemId, String action) {
        Long cartId = cartService.getCurrentCartId();

        Item item = getItemEntityById(itemId);

        cartService.updateItemCount(cartId, item, action);

        Map<Long, Integer> cartItemCounts = cartService.getItemCounts(cartId);
        int updatedCount = cartItemCounts.getOrDefault(itemId, 0);

        return itemMapper.toDto(item, updatedCount);
    }

    @Override
    public Item getItemEntityById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Товар не найден"));
    }

    private String buildRedirectUrl(String search, String sort, int pageNumber, int pageSize) {
        StringBuilder redirectUrl = new StringBuilder("/items?");

        if (search != null && !search.isEmpty()) {
            redirectUrl.append("search=").append(search).append("&");
        }

        redirectUrl.append("sort=").append(sort).append("&");
        redirectUrl.append("pageNumber=").append(pageNumber).append("&");
        redirectUrl.append("pageSize=").append(pageSize);

        return "redirect:" + redirectUrl;
    }


    private Page<Item> getItemsPageWithSearch(String search, Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            return itemRepository.findAll(pageable);
        } else {
            return itemRepository.searchItems(search.trim(), pageable);
        }
    }
}