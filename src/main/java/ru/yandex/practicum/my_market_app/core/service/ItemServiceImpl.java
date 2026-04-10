package ru.yandex.practicum.my_market_app.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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


    private Page<Item> getItemsPageWithSearch(String search, Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            return itemRepository.findAll(pageable);
        } else {
            return itemRepository.searchItems(search.trim(), pageable);
        }
    }
}