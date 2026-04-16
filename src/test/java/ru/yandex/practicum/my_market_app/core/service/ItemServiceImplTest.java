package ru.yandex.practicum.my_market_app.core.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.my_market_app.api.handler.ItemNotFoundException;
import ru.yandex.practicum.my_market_app.configuration.AbstractTestcontainersTest;
import ru.yandex.practicum.my_market_app.core.model.ItemDto;
import ru.yandex.practicum.my_market_app.core.model.ItemsPageData;
import ru.yandex.practicum.my_market_app.persistence.entity.Cart;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;
import ru.yandex.practicum.my_market_app.persistence.repository.CartRepository;
import ru.yandex.practicum.my_market_app.persistence.repository.ItemRepository;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Интеграционные тесты сервиса товаров с реальной БД")
class ItemServiceImplTest extends AbstractTestcontainersTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartRepository cartRepository;

    private Cart testCart;
    private Item testItem1;
    private Item testItem2;
    private Item testItem3;
    private String testSessionId;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll().block();
        itemRepository.deleteAll().block();

        testSessionId = "test-session-" + System.currentTimeMillis();

        testCart = new Cart();
        testCart.setSessionId(testSessionId);
        testCart = cartRepository.save(testCart).block();

        testItem1 = new Item();
        testItem1.setTitle("Апельсин");
        testItem1.setDescription("Сочный апельсин");
        testItem1.setImgPath("/images/orange.jpg");
        testItem1.setPrice(100L);
        testItem1 = itemRepository.save(testItem1).block();

        testItem2 = new Item();
        testItem2.setTitle("Банан");
        testItem2.setDescription("Сладкий банан");
        testItem2.setImgPath("/images/banana.jpg");
        testItem2.setPrice(200L);
        testItem2 = itemRepository.save(testItem2).block();

        testItem3 = new Item();
        testItem3.setTitle("Яблоко");
        testItem3.setDescription("Хрустящее яблоко");
        testItem3.setImgPath("/images/apple.jpg");
        testItem3.setPrice(150L);
        testItem3 = itemRepository.save(testItem3).block();
    }

    @AfterEach
    void tearDown() {
        cartRepository.deleteAll().block();
        itemRepository.deleteAll().block();
    }

    @Nested
    @DisplayName("Тесты получения страницы товаров")
    class GetItemsPageTests {

        @Test
        @DisplayName("Должен вернуть страницу товаров без поиска и сортировки")
        void shouldReturnItemsPageWithoutSearchAndSort() {
            StepVerifier.create(itemService.getItemsPage("", "NO", 1, 10, testSessionId))
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
        @DisplayName("Должен вернуть страницу товаров с сортировкой по названию ALPHA")
        void shouldReturnItemsPageWithAlphaSort() {
            StepVerifier.create(itemService.getItemsPage("", "ALPHA", 1, 10, testSessionId))
                    .assertNext(pageData -> {
                        assertThat(pageData).isNotNull();
                        assertThat(pageData.getSort()).isEqualTo("ALPHA");

                        List<List<ItemDto>> grid = pageData.getItemsGrid();
                        List<ItemDto> items = grid.stream()
                                .flatMap(List::stream)
                                .filter(item -> item.id() != -1)
                                .toList();

                        assertThat(items).isNotEmpty();
                        assertThat(items.get(0).title()).isEqualTo("Апельсин");
                        assertThat(items.get(1).title()).isEqualTo("Банан");
                        assertThat(items.get(2).title()).isEqualTo("Яблоко");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен вернуть страницу товаров с сортировкой по цене PRICE")
        void shouldReturnItemsPageWithPriceSort() {
            StepVerifier.create(itemService.getItemsPage("", "PRICE", 1, 10, testSessionId))
                    .assertNext(pageData -> {
                        assertThat(pageData).isNotNull();
                        assertThat(pageData.getSort()).isEqualTo("PRICE");

                        List<List<ItemDto>> grid = pageData.getItemsGrid();
                        List<ItemDto> items = grid.stream()
                                .flatMap(List::stream)
                                .filter(item -> item.id() != -1)
                                .toList();

                        assertThat(items).isNotEmpty();
                        assertThat(items.get(0).price()).isEqualTo(100L);
                        assertThat(items.get(1).price()).isEqualTo(150L);
                        assertThat(items.get(2).price()).isEqualTo(200L);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен вернуть страницу товаров с поиском")
        void shouldReturnItemsPageWithSearch() {
            StepVerifier.create(itemService.getItemsPage("яблоко", "NO", 1, 10, testSessionId))
                    .assertNext(pageData -> {
                        assertThat(pageData).isNotNull();
                        assertThat(pageData.getSearch()).isEqualTo("яблоко");

                        List<List<ItemDto>> grid = pageData.getItemsGrid();
                        List<ItemDto> items = grid.stream()
                                .flatMap(List::stream)
                                .filter(item -> item.id() != -1)
                                .toList();

                        assertThat(items).hasSize(1);
                        assertThat(items.getFirst().title()).isEqualTo("Яблоко");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен вернуть пустой список при поиске по несуществующему товару")
        void shouldReturnEmptyListWhenSearchNotFound() {
            StepVerifier.create(itemService.getItemsPage("несуществующий товар", "NO", 1, 10, testSessionId))
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
            StepVerifier.create(itemService.getItemsPage("", "NO", 1, 10, testSessionId))
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
            StepVerifier.create(itemService.getItemsPage("", "NO", 1, 2, testSessionId))
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
    @DisplayName("Тесты получения товара по ID")
    class GetItemByIdTests {

        @Test
        @DisplayName("Должен вернуть товар по существующему ID")
        void shouldReturnItemById() {
            StepVerifier.create(itemService.getItemById(testItem1.getId(), testSessionId))
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
        @DisplayName("Должен выбросить исключение при ненайденном товаре")
        void shouldThrowExceptionWhenItemNotFound() {
            StepVerifier.create(itemService.getItemById(99999L, testSessionId))
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
                            .then(itemService.getItemById(testItem1.getId(), testSessionId))
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
                                    testItem1.getId(), "тест", "ALPHA", 2, 10, "PLUS", testSessionId)
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
                    testItem1.getId(), "", "NO", 1, 5, "MINUS", testSessionId).block();

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
                    testItem1.getId(), "", "NO", 1, 5, "DELETE", testSessionId).block();

            assertThat(redirectUrl).isNotNull();
            assertThat(redirectUrl).contains("/items?sort=NO&pageNumber=1&pageSize=5");

            var counts = cartService.getItemCounts(testCart.getId()).block();
            assertThat(Objects.requireNonNull(counts).getOrDefault(testItem1.getId(), 0)).isZero();
        }

        @Test
        @DisplayName("Должен выбросить исключение при обновлении несуществующего товара")
        void shouldThrowExceptionWhenUpdatingNonExistentItem() {
            StepVerifier.create(
                            itemService.updateCartItemAndGetRedirectUrl(99999L, "", "NO", 1, 5, "PLUS", testSessionId)
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
            StepVerifier.create(itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS", testSessionId))
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
                    itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS", testSessionId)
                            .then(itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS", testSessionId))
                            .then(itemService.updateItemCountAndGetItem(testItem1.getId(), "MINUS", testSessionId))
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
                    itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS", testSessionId)
                            .then(itemService.updateItemCountAndGetItem(testItem1.getId(), "DELETE", testSessionId))
            ).assertNext(updatedItem -> {
                assertThat(updatedItem).isNotNull();
                assertThat(updatedItem.id()).isEqualTo(testItem1.getId());
                assertThat(updatedItem.count()).isZero();
            }).verifyComplete();
        }

        @Test
        @DisplayName("Должен выбросить исключение при обновлении несуществующего товара")
        void shouldThrowExceptionWhenUpdatingNonExistentItem() {
            StepVerifier.create(itemService.updateItemCountAndGetItem(99999L, "PLUS", testSessionId))
                    .expectErrorMatches(throwable -> throwable instanceof ItemNotFoundException &&
                            throwable.getMessage().contains("Товар не найден"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("Тесты получения сущности товара по ID")
    class GetItemEntityByIdTests {

        @Test
        @DisplayName("Должен вернуть сущность товара по существующему ID")
        void shouldReturnItemEntityById() {
            StepVerifier.create(itemService.getItemEntityById(testItem1.getId()))
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
            StepVerifier.create(itemService.getItemEntityById(99999L))
                    .expectErrorMatches(throwable -> throwable instanceof ItemNotFoundException &&
                            throwable.getMessage().contains("Товар не найден"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("Тесты работы с разными сессиями")
    class DifferentSessionsTests {

        @Test
        @DisplayName("Разные сессии должны иметь разные корзины")
        void differentSessionsShouldHaveDifferentCarts() {
            String sessionId1 = "session-1-" + System.currentTimeMillis();
            String sessionId2 = "session-2-" + System.currentTimeMillis();

            StepVerifier.create(
                    itemService.updateCartItemAndGetRedirectUrl(testItem1.getId(), "", "NO", 1, 5, "PLUS", sessionId1)
                            .then(itemService.updateCartItemAndGetRedirectUrl(testItem2.getId(), "", "NO", 1, 5, "PLUS", sessionId2))
                            .then(Mono.zip(
                                    itemService.getItemsPage("", "NO", 1, 10, sessionId1),
                                    itemService.getItemsPage("", "NO", 1, 10, sessionId2)
                            ))
            ).assertNext(tuple -> {
                ItemsPageData pageData1 = tuple.getT1();
                ItemsPageData pageData2 = tuple.getT2();

                List<ItemDto> items1 = pageData1.getItemsGrid().stream()
                        .flatMap(List::stream)
                        .filter(item -> Objects.equals(item.id(), testItem1.getId()))
                        .toList();

                List<ItemDto> items2 = pageData2.getItemsGrid().stream()
                        .flatMap(List::stream)
                        .filter(item -> Objects.equals(item.id(), testItem2.getId()))
                        .toList();

                assertThat(items1.getFirst().count()).isEqualTo(1);
                assertThat(items2.getFirst().count()).isEqualTo(1);
            }).verifyComplete();
        }
    }

    @Nested
    @DisplayName("Интеграционные тесты потока данных")
    class IntegrationFlowTests {

        @Test
        @DisplayName("Должен корректно обработать полный цикл операций с товарами и корзиной")
        void shouldHandleFullItemAndCartCycle() {
            StepVerifier.create(
                    itemService.getItemsPage("", "NO", 1, 10, testSessionId)
                            .flatMap(pageData -> {
                                assertThat(pageData.getItemsGrid()).isNotEmpty();
                                return itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS", testSessionId);
                            })
                            .flatMap(updatedItem -> {
                                assertThat(updatedItem.count()).isEqualTo(1);
                                return itemService.getItemsPage("", "NO", 1, 10, testSessionId);
                            })
                            .flatMap(updatedPageData -> {
                                List<List<ItemDto>> grid = updatedPageData.getItemsGrid();
                                List<ItemDto> items = grid.stream()
                                        .flatMap(List::stream)
                                        .filter(item -> item.id().equals(testItem1.getId()))
                                        .toList();
                                assertThat(items.getFirst().count()).isEqualTo(1);
                                return itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS", testSessionId);
                            })
                            .flatMap(updatedItem -> {
                                assertThat(updatedItem.count()).isEqualTo(2);
                                return itemService.updateItemCountAndGetItem(testItem1.getId(), "DELETE", testSessionId);
                            })
            ).assertNext(updatedItem -> assertThat(updatedItem.count()).isZero()).verifyComplete();
        }

        @Test
        @DisplayName("Должен корректно обработать поиск и сортировку вместе")
        void shouldHandleSearchAndSortTogether() {
            StepVerifier.create(
                    cartService.updateItemCount(testCart.getId(), testItem2, "PLUS")
                            .then(cartService.updateItemCount(testCart.getId(), testItem3, "PLUS"))
                            .then(cartService.updateItemCount(testCart.getId(), testItem3, "PLUS"))
                            .then(itemService.getItemsPage("яблоко", "PRICE", 1, 10, testSessionId))
            ).assertNext(pageData -> {
                assertThat(pageData.getSearch()).isEqualTo("яблоко");
                assertThat(pageData.getSort()).isEqualTo("PRICE");

                List<List<ItemDto>> grid = pageData.getItemsGrid();
                List<ItemDto> items = grid.stream()
                        .flatMap(List::stream)
                        .filter(item -> item.id() != -1)
                        .toList();

                assertThat(items).hasSize(1);
                assertThat(items.getFirst().title()).isEqualTo("Яблоко");
                assertThat(items.getFirst().count()).isEqualTo(2);
            }).verifyComplete();
        }

        @Test
        @DisplayName("Должен корректно обработать пагинацию")
        void shouldHandlePagination() {
            StepVerifier.create(
                    Mono.zip(
                            itemService.getItemsPage("", "NO", 1, 2, testSessionId),
                            itemService.getItemsPage("", "NO", 2, 2, testSessionId)
                    )
            ).assertNext(tuple -> {
                ItemsPageData firstPage = tuple.getT1();
                ItemsPageData secondPage = tuple.getT2();

                assertThat(firstPage.getPaging().pageNumber()).isEqualTo(1);
                assertThat(secondPage.getPaging().pageNumber()).isEqualTo(2);

                List<ItemDto> firstPageItems = firstPage.getItemsGrid().stream()
                        .flatMap(List::stream)
                        .filter(item -> item.id() != -1)
                        .toList();
                List<ItemDto> secondPageItems = secondPage.getItemsGrid().stream()
                        .flatMap(List::stream)
                        .filter(item -> item.id() != -1)
                        .toList();

                assertThat(firstPageItems).isNotEmpty();
                assertThat(secondPageItems).isNotEmpty();
                assertThat(firstPageItems.getFirst().id()).isNotEqualTo(secondPageItems.getFirst().id());
            }).verifyComplete();
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
                        return itemService.getItemsPage("", "NO", 1, 10, testSessionId);
                    })
            ).assertNext(pageData -> {
                List<List<ItemDto>> grid = pageData.getItemsGrid();
                assertThat(grid.size()).isGreaterThanOrEqualTo(3);
            }).verifyComplete();
        }

        @Test
        @DisplayName("Должен создать сетку с заглушками для пустых ячеек")
        void shouldCreateGridWithPlaceholdersForEmptyCells() {
            StepVerifier.create(itemService.getItemsPage("", "NO", 1, 2, testSessionId))
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