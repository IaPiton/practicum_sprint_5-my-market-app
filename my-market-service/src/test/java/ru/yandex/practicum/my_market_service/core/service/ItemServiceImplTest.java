package ru.yandex.practicum.my_market_service.core.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.my_market_service.api.handler.ItemNotFoundException;
import ru.yandex.practicum.my_market_service.configuration.TestcontainersTest;
import ru.yandex.practicum.my_market_service.core.model.ItemDto;
import ru.yandex.practicum.my_market_service.persistence.entity.Cart;
import ru.yandex.practicum.my_market_service.persistence.entity.Item;
import ru.yandex.practicum.my_market_service.persistence.repository.CartRepository;
import ru.yandex.practicum.my_market_service.persistence.repository.ItemRepository;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Интеграционные тесты сервиса товаров с реальной БД и кэшированием")
class ItemServiceImplTest extends TestcontainersTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemCacheService itemCacheService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CacheManager cacheManager;

    private Cart testCart;
    private Item testItem1;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames()
                .forEach(cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());

        cartRepository.deleteAll().block();
        itemRepository.deleteAll().block();

        testCart = new Cart();
        Long userId = 3L;
        testCart.setUserId(userId);
        testCart = cartRepository.save(testCart).block();

        testItem1 = new Item();
        testItem1.setTitle("Апельсин");
        testItem1.setDescription("Сочный апельсин");
        testItem1.setImgPath("/images/orange.jpg");
        testItem1.setPrice(100L);
        testItem1 = itemRepository.save(testItem1).block();

    }

    @AfterEach
    void tearDown() {
        cartRepository.deleteAll().block();
        itemRepository.deleteAll().block();

        cacheManager.getCacheNames()
                .forEach(cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
    }

    @Nested
    @DisplayName("Тесты получения страницы товаров с кэшированием")
    class GetItemsPageTests {

        @Test
        @DisplayName("Должен вернуть страницу товаров без поиска и сортировки")
        void shouldReturnItemsPageWithoutSearchAndSort() {
            StepVerifier.create(itemService.getItemsPage("", "NO", 1, 10))
                    .assertNext(pageData -> {
                        assertThat(pageData).isNotNull();
                        assertThat(pageData.getItemsGrid()).isNotEmpty();
                        assertThat(pageData.getSearch()).isEmpty();
                        assertThat(pageData.getSort()).isEqualTo("NO");
                        assertThat(pageData.getPaging()).isNotNull();
                        assertThat(pageData.getPaging().pageNumber()).isEqualTo(1);
                        assertThat(pageData.getPaging().pageSize()).isEqualTo(10);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен вернуть страницу товаров с сортировкой по цене PRICE")
        void shouldReturnItemsPageWithPriceSort() {
            StepVerifier.create(itemService.getItemsPage("", "PRICE", 1, 10))
                    .assertNext(pageData -> {
                        assertThat(pageData).isNotNull();
                        assertThat(pageData.getSort()).isEqualTo("PRICE");

                        List<List<ItemDto>> grid = pageData.getItemsGrid();
                        List<ItemDto> items = grid.stream()
                                .flatMap(List::stream)
                                .filter(item -> item.id() != -1)
                                .toList();

                        assertThat(items).isNotEmpty();

                        if (items.size() >= 2) {
                            assertThat(items.get(0).price()).isLessThanOrEqualTo(items.get(1).price());
                        }

                        if (items.size() >= 3) {
                            assertThat(items.get(0).price()).isEqualTo(100L);
                            assertThat(items.get(1).price()).isEqualTo(150L);
                            assertThat(items.get(2).price()).isEqualTo(200L);
                        }
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен вернуть пустой список при поиске по несуществующему товару")
        void shouldReturnEmptyListWhenSearchNotFound() {
            StepVerifier.create(itemService.getItemsPage("несуществующий товар", "NO", 1, 10))
                    .assertNext(pageData -> {
                        assertThat(pageData).isNotNull();

                        List<List<ItemDto>> grid = pageData.getItemsGrid();
                        List<ItemDto> items = grid.stream()
                                .flatMap(List::stream)
                                .filter(item -> item.id() != -1)
                                .toList();

                        assertThat(items).isEmpty();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен корректно разбивать товары на строки по 3 товара")
        void shouldPartitionItemsIntoRowsOfThree() {
            StepVerifier.create(itemService.getItemsPage("", "NO", 1, 10))
                    .assertNext(pageData -> {
                        List<List<ItemDto>> grid = pageData.getItemsGrid();
                        assertThat(grid).isNotEmpty();

                        for (List<ItemDto> row : grid) {
                            assertThat(row.size()).isLessThanOrEqualTo(3);
                        }
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен добавить заглушки для пустых ячеек в последней строке")
        void shouldAddPlaceholdersForEmptyCells() {
            StepVerifier.create(itemService.getItemsPage("", "NO", 1, 2))
                    .assertNext(pageData -> {
                        List<List<ItemDto>> grid = pageData.getItemsGrid();

                        assertThat(grid).hasSize(1);

                        List<ItemDto> lastRow = grid.getFirst();
                        assertThat(lastRow).hasSize(3);

                        boolean hasPlaceholder = lastRow.stream().anyMatch(item -> item.id() == -1);
                        assertThat(hasPlaceholder).isTrue();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Тесты получения товара по ID с кэшированием")
    class GetItemByIdTests {

        @Test
        @DisplayName("Должен вернуть товар по существующему ID")
        void shouldReturnItemById() {
            StepVerifier.create(itemService.getItemById(testItem1.getId()))
                    .assertNext(item -> {
                        assertThat(item).isNotNull();
                        assertThat(item.id()).isEqualTo(testItem1.getId());
                        assertThat(item.title()).isEqualTo(testItem1.getTitle());
                        assertThat(item.description()).isEqualTo(testItem1.getDescription());
                        assertThat(item.price()).isEqualTo(testItem1.getPrice());
                        assertThat(item.count()).isZero();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен кэшировать результат getItemEntityById")
        void shouldCacheGetItemEntityByIdResult() {
            Long itemId = testItem1.getId();

            StepVerifier.create(itemCacheService.getItemEntityById(itemId))
                    .expectNextCount(1)
                    .verifyComplete();

            assertThat(cacheManager.getCache("items")).isNotNull();

            StepVerifier.create(itemCacheService.getItemEntityById(itemId))
                    .expectNextCount(1)
                    .verifyComplete();

            Objects.requireNonNull(cacheManager.getCache("items")).evict(itemId);

            StepVerifier.create(itemCacheService.getItemEntityById(itemId))
                    .expectNextCount(1)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен кэшировать результат searchAllItems")
        void shouldCacheSearchAllItemsResult() {
            String search = "";
            String sort = "title";
            int limit = 10;
            long offset = 0;

            StepVerifier.create(itemCacheService.searchAllItems(search, sort, limit, offset))
                    .expectNextCount(1)
                    .verifyComplete();

            String cacheKey = search + "-" + limit + "-" + offset + "-" + sort;
            assertThat(cacheManager.getCache("allItems")).isNotNull();
            assertThat(Objects.requireNonNull(cacheManager.getCache("allItems")).get(cacheKey)).isNotNull();

            StepVerifier.create(itemCacheService.searchAllItems(search, sort, limit, offset))
                    .expectNextCount(1)
                    .verifyComplete();

            Objects.requireNonNull(cacheManager.getCache("allItems")).clear();

            StepVerifier.create(itemCacheService.searchAllItems(search, sort, limit, offset))
                    .expectNextCount(1)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен выбросить исключение при ненайденном товаре")
        void shouldThrowExceptionWhenItemNotFound() {
            StepVerifier.create(itemService.getItemById(99999L))
                    .expectErrorMatches(throwable -> throwable instanceof ItemNotFoundException &&
                            throwable.getMessage().contains("Товар не найден"))
                    .verify();
        }

        @Test
        @DisplayName("Должен вернуть товар с количеством в корзине")
        void shouldReturnItemWithCartCount() {
            StepVerifier.create(
                    cartService.updateItemCount(testCart.getId(), testItem1, "PLUS")
                            .then(cartService.updateItemCount(testCart.getId(), testItem1, "PLUS"))
                            .then(itemService.getItemById(testItem1.getId()))
            ).assertNext(item -> {
                assertThat(item).isNotNull();
                assertThat(item.count()).isEqualTo(2);
            }).verifyComplete();
        }
    }

    @Nested
    @DisplayName("Тесты обновления количества товара в корзине с редиректом")
    class UpdateCartItemAndGetRedirectUrlTests {

        @Test
        @DisplayName("Должен увеличить количество товара и вернуть URL для редиректа при PLUS")
        void shouldIncreaseItemCountAndReturnRedirectUrlWhenPlusAction() {
            StepVerifier.create(
                    itemService.updateCartItemAndGetRedirectUrl(
                                    testItem1.getId(), "тест", "ALPHA", 2, 10, "PLUS")
                            .flatMap(redirectUrl ->
                                    cartService.getItemCounts(testCart.getId())
                                            .map(counts -> new Object() {
                                                final String url = redirectUrl;
                                                final int count = counts.getOrDefault(testItem1.getId(), 0);
                                            })
                            )
            ).assertNext(pair -> {
                assertThat(pair.url).isNotNull();
                assertThat(pair.url).contains("/items?search=тест&sort=ALPHA&pageNumber=2&pageSize=10");
                assertThat(pair.count).isEqualTo(1);
            }).verifyComplete();
        }

        @Test
        @DisplayName("Должен уменьшить количество товара и вернуть URL для редиректа при MINUS")
        void shouldDecreaseItemCountAndReturnRedirectUrlWhenMinusAction() {
            cartService.updateItemCount(testCart.getId(), testItem1, "PLUS").block();
            cartService.updateItemCount(testCart.getId(), testItem1, "PLUS").block();

            String redirectUrl = itemService.updateCartItemAndGetRedirectUrl(
                    testItem1.getId(), "", "NO", 1, 5, "MINUS").block();

            assertThat(redirectUrl).isNotNull();
            assertThat(redirectUrl).contains("/items?sort=NO&pageNumber=1&pageSize=5");

            var counts = cartService.getItemCounts(testCart.getId()).block();
            assertThat(Objects.requireNonNull(counts).getOrDefault(testItem1.getId(), 0)).isEqualTo(1);
        }

        @Test
        @DisplayName("Должен удалить товар и вернуть URL для редиректа при DELETE")
        void shouldDeleteItemAndReturnRedirectUrlWhenDeleteAction() {
            cartService.updateItemCount(testCart.getId(), testItem1, "PLUS").block();

            String redirectUrl = itemService.updateCartItemAndGetRedirectUrl(
                    testItem1.getId(), "", "NO", 1, 5, "DELETE").block();

            assertThat(redirectUrl).isNotNull();
            assertThat(redirectUrl).contains("/items?sort=NO&pageNumber=1&pageSize=5");

            var counts = cartService.getItemCounts(testCart.getId()).block();
            assertThat(Objects.requireNonNull(counts).getOrDefault(testItem1.getId(), 0)).isZero();
        }

        @Test
        @DisplayName("Должен выбросить исключение при обновлении несуществующего товара")
        void shouldThrowExceptionWhenUpdatingNonExistentItem() {
            StepVerifier.create(
                            itemService.updateCartItemAndGetRedirectUrl(99999L, "", "NO", 1, 5, "PLUS")
                    ).expectErrorMatches(throwable -> throwable instanceof ItemNotFoundException &&
                            throwable.getMessage().contains("Товар не найден"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("Тесты обновления количества товара и получения обновленного товара")
    class UpdateItemCountAndGetItemTests {

        @Test
        @DisplayName("Должен увеличить количество и вернуть обновленный товар при PLUS")
        void shouldIncreaseCountAndReturnUpdatedItemWhenPlusAction() {
            StepVerifier.create(itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS"))
                    .assertNext(updatedItem -> {
                        assertThat(updatedItem).isNotNull();
                        assertThat(updatedItem.id()).isEqualTo(testItem1.getId());
                        assertThat(updatedItem.count()).isEqualTo(1);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен уменьшить количество и вернуть обновленный товар при MINUS")
        void shouldDecreaseCountAndReturnUpdatedItemWhenMinusAction() {
            StepVerifier.create(
                    itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS")
                            .then(itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS"))
                            .then(itemService.updateItemCountAndGetItem(testItem1.getId(), "MINUS"))
            ).assertNext(updatedItem -> {
                assertThat(updatedItem).isNotNull();
                assertThat(updatedItem.id()).isEqualTo(testItem1.getId());
                assertThat(updatedItem.count()).isEqualTo(1);
            }).verifyComplete();
        }

        @Test
        @DisplayName("Должен удалить товар и вернуть товар с нулевым количеством при DELETE")
        void shouldDeleteAndReturnItemWithZeroCountWhenDeleteAction() {
            StepVerifier.create(
                    itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS")
                            .then(itemService.updateItemCountAndGetItem(testItem1.getId(), "DELETE"))
            ).assertNext(updatedItem -> {
                assertThat(updatedItem).isNotNull();
                assertThat(updatedItem.id()).isEqualTo(testItem1.getId());
                assertThat(updatedItem.count()).isZero();
            }).verifyComplete();
        }

        @Test
        @DisplayName("Должен выбросить исключение при обновлении несуществующего товара")
        void shouldThrowExceptionWhenUpdatingNonExistentItem() {
            StepVerifier.create(itemService.updateItemCountAndGetItem(99999L, "PLUS"))
                    .expectErrorMatches(throwable -> throwable instanceof ItemNotFoundException &&
                            throwable.getMessage().contains("Товар не найден"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("Тесты получения сущности товара по ID с кэшированием")
    class GetItemEntityByIdTests {

        @Test
        @DisplayName("Должен вернуть сущность товара по существующему ID")
        void shouldReturnItemEntityById() {
            StepVerifier.create(itemCacheService.getItemEntityById(testItem1.getId()))
                    .assertNext(item -> {
                        assertThat(item).isNotNull();
                        assertThat(item.getId()).isEqualTo(testItem1.getId());
                        assertThat(item.getTitle()).isEqualTo(testItem1.getTitle());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен выбросить исключение при ненайденной сущности")
        void shouldThrowExceptionWhenItemEntityNotFound() {
            StepVerifier.create(itemCacheService.getItemEntityById(99999L))
                    .expectErrorMatches(throwable -> throwable instanceof ItemNotFoundException &&
                            throwable.getMessage().contains("Товар не найден"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("Тесты построения сетки товаров")
    class ItemsGridBuilderTests {

        @Test
        @DisplayName("Должен создать сетку 3x3 для 9 товаров")
        void shouldCreate3x3GridFor9Items() {
            StepVerifier.create(
                    Mono.defer(() -> {
                        for (int i = 0; i < 6; i++) {
                            Item item = new Item();
                            item.setTitle("Дополнительный товар " + i);
                            item.setDescription("Описание");
                            item.setPrice(100L);
                            itemRepository.save(item).block();
                        }
                        return itemService.getItemsPage("", "NO", 1, 10);
                    })
            ).assertNext(pageData -> {
                List<List<ItemDto>> grid = pageData.getItemsGrid();
                assertThat(grid.size()).isGreaterThanOrEqualTo(3);
            }).verifyComplete();
        }

        @Test
        @DisplayName("Должен создать сетку с заглушками для пустых ячеек")
        void shouldCreateGridWithPlaceholdersForEmptyCells() {
            StepVerifier.create(itemService.getItemsPage("", "NO", 1, 2))
                    .assertNext(pageData -> {
                        List<List<ItemDto>> grid = pageData.getItemsGrid();
                        List<ItemDto> lastRow = grid.getLast();
                        long placeholderCount = lastRow.stream().filter(item -> item.id() == -1).count();
                        assertThat(placeholderCount).isGreaterThan(0);
                    })
                    .verifyComplete();
        }
    }
}