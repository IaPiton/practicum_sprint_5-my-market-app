package ru.yandex.practicum.my_market_service.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.core.mapper.ItemMapper;
import ru.yandex.practicum.my_market_service.core.mapper.ItemsGridBuilder;
import ru.yandex.practicum.my_market_service.core.model.ItemDto;
import ru.yandex.practicum.my_market_service.core.model.ItemsPageData;
import ru.yandex.practicum.my_market_service.core.model.PagingInfo;
import ru.yandex.practicum.my_market_service.persistence.entity.Item;
import ru.yandex.practicum.my_market_service.persistence.repository.ItemRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final CartService cartService;
    private final ItemMapper itemMapper;
    private final ItemsGridBuilder gridBuilder;
    private final ItemCacheService itemCacheService;

    @Override
    public Mono<ItemsPageData> getItemsPage(String search, String sort, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        long offset = pageable.getOffset();
        int limit = pageable.getPageSize();

        String sortColumn = getSortColumn(sort);

        Mono<List<Item>> itemsFlux;
        Mono<Long> countMono;

        itemsFlux = itemCacheService.searchAllItems(search, sortColumn, limit, offset);

        if (search == null || search.trim().isEmpty()) {
            countMono = itemRepository.count();
        } else {
            countMono = itemRepository.countBySearch(search);
        }

        return itemsFlux
                .zipWith(countMono)
                .flatMap(tuple -> {
                    List<Item> items = tuple.getT1();
                    Long total = tuple.getT2();
                    Page<Item> itemPage = new PageImpl<>(items, pageable, total);

                    return cartService.getCurrentCartId()
                            .flatMap(cartService::getItemCounts)
                            .map(cartItemCounts -> {
                                List<ItemDto> itemsWithCount = itemMapper.toDtoList(
                                        itemPage.getContent(),
                                        cartItemCounts
                                );
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
                            });
                });
    }

    @Override
    public Mono<ItemDto> getItemById(Long itemId) {
        Mono<Map<Long, Integer>> cartItemCountsMono = cartService.getCurrentCartId()
                .flatMap(cartService::getItemCounts);
        Mono<Item> itemMono = itemCacheService.getItemEntityById(itemId);
        return Mono.zip(cartItemCountsMono, itemMono)
                .map(tuple ->
                {
                    Map<Long, Integer> cartItemCounts = tuple.getT1();
                    Item item = tuple.getT2();
                    int updatedCount = cartItemCounts.getOrDefault(item.getId(), 0);
                    return itemMapper.toDto(item, updatedCount);
                });
    }

    @Override
    public Mono<String> updateCartItemAndGetRedirectUrl(Long itemId, String search, String sort,
                                                        int pageNumber, int pageSize, String action) {
        return Mono.zip(cartService.getCurrentCartId(), itemCacheService.getItemEntityById(itemId))
                .flatMap(tuple ->
                {
                    Long cartId = tuple.getT1();
                    Item item = tuple.getT2();
                    return cartService.updateItemCount(cartId, item, action)
                            .thenReturn(buildRedirectUrl(search, sort, pageNumber, pageSize));
                });

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




    @Override
    public Mono<ItemDto> updateItemCountAndGetItem(Long itemId, String action) {
        return Mono.zip(cartService.getCurrentCartId(), itemCacheService.getItemEntityById(itemId))
                .flatMap(tuple -> {
                    Long cartId = tuple.getT1();
                    Item item = tuple.getT2();

                    return cartService.updateItemCount(cartId, item, action)
                            .then(cartService.getItemCounts(cartId))
                            .map(cartItemCounts -> {
                                int updatedCount = cartItemCounts.getOrDefault(itemId, 0);
                                return itemMapper.toDto(item, updatedCount);
                            });
                });

    }

    private String getSortColumn(String sort) {
        return switch (sort) {
            case "ALPHA" -> "title";
            case "PRICE" -> "price";
            default -> "id";
        };
    }
}
