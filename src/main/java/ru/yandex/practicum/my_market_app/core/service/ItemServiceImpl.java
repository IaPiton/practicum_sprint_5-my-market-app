package ru.yandex.practicum.my_market_app.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_app.api.handler.ItemNotFoundException;
import ru.yandex.practicum.my_market_app.core.mapper.ItemMapper;
import ru.yandex.practicum.my_market_app.core.mapper.ItemsGridBuilder;
import ru.yandex.practicum.my_market_app.core.model.ItemDto;
import ru.yandex.practicum.my_market_app.core.model.ItemsPageData;
import ru.yandex.practicum.my_market_app.core.model.PagingInfo;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;
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

    @Override
    public Mono<ItemsPageData> getItemsPage(String search, String sort, int pageNumber, int pageSize, String sessionId) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        long offset = pageable.getOffset();
        int limit = pageable.getPageSize();

        String sortType = (sort == null || sort.equals("NO")) ? "NO" : sort;
        boolean isAscending = true;

        if (sortType.contains("_")) {
            String[] parts = sortType.split("_");
            sortType = parts[0];
            isAscending = parts.length == 1 || parts[1].equals("ASC");
        }

        final String finalSortType = sortType;
        final boolean finalIsAscending = isAscending;

        Flux<Item> itemsFlux;
        Mono<Long> countMono;

        if (search == null || search.trim().isEmpty()) {
            itemsFlux = getItemsFluxNoSearch(finalSortType, finalIsAscending, offset, limit);
            countMono = itemRepository.count();
        } else {
            String searchPattern = "%" + search.trim() + "%";
            itemsFlux = getItemsFluxWithSearch(searchPattern, finalSortType, finalIsAscending, offset, limit);
            countMono = itemRepository.countBySearch(searchPattern);
        }

        return itemsFlux.collectList()
                .zipWith(countMono)
                .flatMap(tuple -> {
                    List<Item> items = tuple.getT1();
                    Long total = tuple.getT2();
                    Page<Item> itemPage = new PageImpl<>(items, pageable, total);

                    return cartService.getCurrentCartId(sessionId)
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
    public Mono<ItemDto> getItemById(Long itemId, String sessionId) {
        Mono<Map<Long, Integer>> cartItemCountsMono = cartService.getCurrentCartId(sessionId)
                .flatMap(cartService::getItemCounts);
        Mono<Item> itemMono = getItemEntityById(itemId);
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
                                                        int pageNumber, int pageSize, String action, String sessionId) {
        return Mono.zip(cartService.getCurrentCartId(sessionId), getItemEntityById(itemId))
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
    public Mono<Item> getItemEntityById(Long itemId) {
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Товар не найден: " + itemId)));
    }

    @Override
    public Mono<ItemDto> updateItemCountAndGetItem(Long itemId, String action, String sessionId) {
        return Mono.zip(cartService.getCurrentCartId(sessionId), getItemEntityById(itemId))
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

    private Flux<Item> getItemsFluxNoSearch(String sortType, boolean ascending, long offset, int limit) {
        return switch (sortType) {
            case "ALPHA" -> ascending
                    ? itemRepository.findAllByTitleAsc(offset, limit)
                    : itemRepository.findAllByTitleDesc(offset, limit);
            case "PRICE" -> ascending
                    ? itemRepository.findAllByPriceAsc(offset, limit)
                    : itemRepository.findAllByPriceDesc(offset, limit);
            default -> itemRepository.findAllNoSort(offset, limit);
        };
    }

    private Flux<Item> getItemsFluxWithSearch(String searchPattern, String sortType, boolean ascending, long offset, int limit) {
        return switch (sortType) {
            case "ALPHA" -> ascending
                    ? itemRepository.searchByTitleAsc(searchPattern, offset, limit)
                    : itemRepository.searchByTitleDesc(searchPattern, offset, limit);
            case "PRICE" -> ascending
                    ? itemRepository.searchByPriceAsc(searchPattern, offset, limit)
                    : itemRepository.searchByPriceDesc(searchPattern, offset, limit);
            default -> itemRepository.searchByTitleNoSort(searchPattern, offset, limit);
        };
    }
}
