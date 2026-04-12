package ru.yandex.practicum.my_market_app.core.service;

import ru.yandex.practicum.my_market_app.configuration.PgTestContainerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.my_market_app.Application;
import ru.yandex.practicum.my_market_app.api.handler.ItemNotFoundException;
import ru.yandex.practicum.my_market_app.core.model.ItemsPageData;
import ru.yandex.practicum.my_market_app.core.model.ItemDto;
import ru.yandex.practicum.my_market_app.persistence.entity.Cart;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;
import ru.yandex.practicum.my_market_app.persistence.repository.CartRepository;
import ru.yandex.practicum.my_market_app.persistence.repository.ItemRepository;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@SpringBootTest(classes = {Application.class, PgTestContainerConfiguration.class})
@ActiveProfiles("test")
@DisplayName("Интеграционные тесты сервиса товаров с реальной БД")
class ItemServiceImplTest {

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
        testSessionId = "test-session-" + System.currentTimeMillis();
        testCart = new Cart();
        testCart.setSessionId(testSessionId);
        testCart = cartRepository.save(testCart);

        testItem1 = new Item();
        testItem1.setTitle("Апельсин");
        testItem1.setDescription("Сочный апельсин");
        testItem1.setImgPath("/images/orange.jpg");
        testItem1.setPrice(100L);
        testItem1 = itemRepository.save(testItem1);

        testItem2 = new Item();
        testItem2.setTitle("Банан");
        testItem2.setDescription("Сладкий банан");
        testItem2.setImgPath("/images/banana.jpg");
        testItem2.setPrice(200L);
        testItem2 = itemRepository.save(testItem2);

        testItem3 = new Item();
        testItem3.setTitle("Яблоко");
        testItem3.setDescription("Хрустящее яблоко");
        testItem3.setImgPath("/images/apple.jpg");
        testItem3.setPrice(150L);
        testItem3 = itemRepository.save(testItem3);
    }

    @Nested
    @DisplayName("Тесты получения страницы товаров")
    class GetItemsPageTests {

        @Test
        @DisplayName("Должен вернуть страницу товаров без поиска и сортировки")
        void shouldReturnItemsPageWithoutSearchAndSort() {
            ItemsPageData pageData = itemService.getItemsPage("", "NO", 1, 10, testSessionId);

            assertThat(pageData).isNotNull();
            assertThat(pageData.getItemsGrid()).isNotEmpty();
            assertThat(pageData.getSearch()).isEmpty();
            assertThat(pageData.getSort()).isEqualTo("NO");
            assertThat(pageData.getPaging()).isNotNull();
            assertThat(pageData.getPaging().pageNumber()).isEqualTo(1);
            assertThat(pageData.getPaging().pageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("Должен вернуть страницу товаров с сортировкой по названию ALPHA")
        void shouldReturnItemsPageWithAlphaSort() {
            ItemsPageData pageData = itemService.getItemsPage("", "ALPHA", 1, 10, testSessionId);

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
        }

        @Test
        @DisplayName("Должен вернуть страницу товаров с сортировкой по цене PRICE")
        void shouldReturnItemsPageWithPriceSort() {
            ItemsPageData pageData = itemService.getItemsPage("", "PRICE", 1, 10, testSessionId);

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
        }

        @Test
        @DisplayName("Должен вернуть страницу товаров с поиском")
        void shouldReturnItemsPageWithSearch() {
            ItemsPageData pageData = itemService.getItemsPage("яблоко", "NO", 1, 10, testSessionId);

            assertThat(pageData).isNotNull();
            assertThat(pageData.getSearch()).isEqualTo("яблоко");

            List<List<ItemDto>> grid = pageData.getItemsGrid();
            List<ItemDto> items = grid.stream()
                    .flatMap(List::stream)
                    .filter(item -> item.id() != -1)
                    .toList();

            assertThat(items).hasSize(1);
            assertThat(items.getFirst().title()).isEqualTo("Яблоко");
        }

        @Test
        @DisplayName("Должен вернуть пустой список при поиске по несуществующему товару")
        void shouldReturnEmptyListWhenSearchNotFound() {
            ItemsPageData pageData = itemService.getItemsPage("несуществующий товар", "NO", 1, 10, testSessionId);

            assertThat(pageData).isNotNull();

            List<List<ItemDto>> grid = pageData.getItemsGrid();
            List<ItemDto> items = grid.stream()
                    .flatMap(List::stream)
                    .filter(item -> item.id() != -1)
                    .toList();

            assertThat(items).isEmpty();
        }

        @Test
        @DisplayName("Должен корректно разбивать товары на строки по 3 товара")
        void shouldPartitionItemsIntoRowsOfThree() {
            ItemsPageData pageData = itemService.getItemsPage("", "NO", 1, 10, testSessionId);

            List<List<ItemDto>> grid = pageData.getItemsGrid();
            assertThat(grid).isNotEmpty();

            for (List<ItemDto> row : grid) {
                assertThat(row.size()).isLessThanOrEqualTo(3);
            }
        }

        @Test
        @DisplayName("Должен добавить заглушки для пустых ячеек в последней строке")
        void shouldAddPlaceholdersForEmptyCells() {
            ItemsPageData pageData = itemService.getItemsPage("", "NO", 1, 2, testSessionId);

            List<List<ItemDto>> grid = pageData.getItemsGrid();

            assertThat(grid).hasSize(1);

            List<ItemDto> lastRow = grid.getFirst();
            assertThat(lastRow).hasSize(3);

            boolean hasPlaceholder = lastRow.stream().anyMatch(item -> item.id() == -1);
            assertThat(hasPlaceholder).isTrue();
        }
    }

    @Nested
    @DisplayName("Тесты получения товара по ID")
    class GetItemByIdTests {

        @Test
        @DisplayName("Должен вернуть товар по существующему ID")
        void shouldReturnItemById() {
            ItemDto item = itemService.getItemById(testItem1.getId(), testSessionId);

            assertThat(item).isNotNull();
            assertThat(item.id()).isEqualTo(testItem1.getId());
            assertThat(item.title()).isEqualTo(testItem1.getTitle());
            assertThat(item.description()).isEqualTo(testItem1.getDescription());
            assertThat(item.price()).isEqualTo(testItem1.getPrice());
            assertThat(item.count()).isZero();
        }

        @Test
        @DisplayName("Должен выбросить исключение при ненайденном товаре")
        void shouldThrowExceptionWhenItemNotFound() {
            assertThatThrownBy(() -> itemService.getItemById(99999L, testSessionId))
                    .isInstanceOf(ItemNotFoundException.class)
                    .hasMessageContaining("Товар не найден");
        }

        @Test
        @DisplayName("Должен вернуть товар с количеством в корзине")
        void shouldReturnItemWithCartCount() {
            cartService.updateItemCount(testCart.getId(), testItem1, "PLUS");
            cartService.updateItemCount(testCart.getId(), testItem1, "PLUS");

            ItemDto item = itemService.getItemById(testItem1.getId(), testSessionId);

            assertThat(item).isNotNull();
            assertThat(item.count()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Тесты обновления количества товара в корзине с редиректом")
    class UpdateCartItemAndGetRedirectUrlTests {

        @Test
        @DisplayName("Должен увеличить количество товара и вернуть URL для редиректа при PLUS")
        void shouldIncreaseItemCountAndReturnRedirectUrlWhenPlusAction() {
            String redirectUrl = itemService.updateCartItemAndGetRedirectUrl(
                    testItem1.getId(), "тест", "ALPHA", 2, 10, "PLUS", testSessionId);

            assertThat(redirectUrl).isNotNull();
            assertThat(redirectUrl).contains("/items?search=тест&sort=ALPHA&pageNumber=2&pageSize=10");

            int count = cartService.getItemCounts(testCart.getId()).getOrDefault(testItem1.getId(), 0);
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Должен уменьшить количество товара и вернуть URL для редиректа при MINUS")
        void shouldDecreaseItemCountAndReturnRedirectUrlWhenMinusAction() {
            cartService.updateItemCount(testCart.getId(), testItem1, "PLUS");
            cartService.updateItemCount(testCart.getId(), testItem1, "PLUS");

            String redirectUrl = itemService.updateCartItemAndGetRedirectUrl(
                    testItem1.getId(), "", "NO", 1, 5, "MINUS", testSessionId);

            assertThat(redirectUrl).isNotNull();
            assertThat(redirectUrl).contains("/items?sort=NO&pageNumber=1&pageSize=5");

            int count = cartService.getItemCounts(testCart.getId()).getOrDefault(testItem1.getId(), 0);
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Должен удалить товар и вернуть URL для редиректа при DELETE")
        void shouldDeleteItemAndReturnRedirectUrlWhenDeleteAction() {
            cartService.updateItemCount(testCart.getId(), testItem1, "PLUS");

            String redirectUrl = itemService.updateCartItemAndGetRedirectUrl(
                    testItem1.getId(), "", "NO", 1, 5, "DELETE", testSessionId);

            assertThat(redirectUrl).isNotNull();

            int count = cartService.getItemCounts(testCart.getId()).getOrDefault(testItem1.getId(), 0);
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Должен выбросить исключение при обновлении несуществующего товара")
        void shouldThrowExceptionWhenUpdatingNonExistentItem() {
            assertThatThrownBy(() -> itemService.updateCartItemAndGetRedirectUrl(
                    99999L, "", "NO", 1, 5, "PLUS", testSessionId))
                    .isInstanceOf(ItemNotFoundException.class)
                    .hasMessageContaining("Товар не найден");
        }
    }

    @Nested
    @DisplayName("Тесты обновления количества товара и получения обновленного товара")
    class UpdateItemCountAndGetItemTests {

        @Test
        @DisplayName("Должен увеличить количество и вернуть обновленный товар при PLUS")
        void shouldIncreaseCountAndReturnUpdatedItemWhenPlusAction() {
            ItemDto updatedItem = itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS", testSessionId);

            assertThat(updatedItem).isNotNull();
            assertThat(updatedItem.id()).isEqualTo(testItem1.getId());
            assertThat(updatedItem.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Должен уменьшить количество и вернуть обновленный товар при MINUS")
        void shouldDecreaseCountAndReturnUpdatedItemWhenMinusAction() {
            itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS", testSessionId);
            itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS", testSessionId);

            ItemDto updatedItem = itemService.updateItemCountAndGetItem(testItem1.getId(), "MINUS", testSessionId);

            assertThat(updatedItem).isNotNull();
            assertThat(updatedItem.id()).isEqualTo(testItem1.getId());
            assertThat(updatedItem.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Должен удалить товар и вернуть товар с нулевым количеством при DELETE")
        void shouldDeleteAndReturnItemWithZeroCountWhenDeleteAction() {
            itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS", testSessionId);

            ItemDto updatedItem = itemService.updateItemCountAndGetItem(testItem1.getId(), "DELETE", testSessionId);

            assertThat(updatedItem).isNotNull();
            assertThat(updatedItem.id()).isEqualTo(testItem1.getId());
            assertThat(updatedItem.count()).isZero();
        }

        @Test
        @DisplayName("Должен выбросить исключение при обновлении несуществующего товара")
        void shouldThrowExceptionWhenUpdatingNonExistentItem() {
            assertThatThrownBy(() -> itemService.updateItemCountAndGetItem(99999L, "PLUS", testSessionId))
                    .isInstanceOf(ItemNotFoundException.class)
                    .hasMessageContaining("Товар не найден");
        }
    }

    @Nested
    @DisplayName("Тесты получения сущности товара по ID")
    class GetItemEntityByIdTests {

        @Test
        @DisplayName("Должен вернуть сущность товара по существующему ID")
        void shouldReturnItemEntityById() {
            Item item = itemService.getItemEntityById(testItem1.getId());

            assertThat(item).isNotNull();
            assertThat(item.getId()).isEqualTo(testItem1.getId());
            assertThat(item.getTitle()).isEqualTo(testItem1.getTitle());
        }

        @Test
        @DisplayName("Должен выбросить исключение при ненайденной сущности")
        void shouldThrowExceptionWhenItemEntityNotFound() {
            assertThatThrownBy(() -> itemService.getItemEntityById(99999L))
                    .isInstanceOf(ItemNotFoundException.class)
                    .hasMessageContaining("Товар не найден");
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

            itemService.updateCartItemAndGetRedirectUrl(testItem1.getId(), "", "NO", 1, 5, "PLUS", sessionId1);
            itemService.updateCartItemAndGetRedirectUrl(testItem2.getId(), "", "NO", 1, 5, "PLUS", sessionId2);

            ItemsPageData pageData1 = itemService.getItemsPage("", "NO", 1, 10, sessionId1);
            ItemsPageData pageData2 = itemService.getItemsPage("", "NO", 1, 10, sessionId2);

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
        }
    }

    @Nested
    @DisplayName("Интеграционные тесты потока данных")
    class IntegrationFlowTests {

        @Test
        @DisplayName("Должен корректно обработать полный цикл операций с товарами и корзиной")
        void shouldHandleFullItemAndCartCycle() {
            ItemsPageData pageData = itemService.getItemsPage("", "NO", 1, 10, testSessionId);
            assertThat(pageData.getItemsGrid()).isNotEmpty();

            ItemDto updatedItem = itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS", testSessionId);
            assertThat(updatedItem.count()).isEqualTo(1);

            ItemsPageData updatedPageData = itemService.getItemsPage("", "NO", 1, 10, testSessionId);
            List<List<ItemDto>> grid = updatedPageData.getItemsGrid();
            List<ItemDto> items = grid.stream()
                    .flatMap(List::stream)
                    .filter(item -> item.id().equals(testItem1.getId()))
                    .toList();
            assertThat(items.getFirst().count()).isEqualTo(1);

            updatedItem = itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS", testSessionId);
            assertThat(updatedItem.count()).isEqualTo(2);

            updatedItem = itemService.updateItemCountAndGetItem(testItem1.getId(), "DELETE", testSessionId);
            assertThat(updatedItem.count()).isZero();
        }

        @Test
        @DisplayName("Должен корректно обработать поиск и сортировку вместе")
        void shouldHandleSearchAndSortTogether() {
            cartService.updateItemCount(testCart.getId(), testItem2, "PLUS");
            cartService.updateItemCount(testCart.getId(), testItem3, "PLUS");
            cartService.updateItemCount(testCart.getId(), testItem3, "PLUS");

            ItemsPageData pageData = itemService.getItemsPage("яблоко", "PRICE", 1, 10, testSessionId);
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
        }

        @Test
        @DisplayName("Должен корректно обработать пагинацию")
        void shouldHandlePagination() {
            ItemsPageData firstPage = itemService.getItemsPage("", "NO", 1, 2, testSessionId);
            assertThat(firstPage.getPaging().pageNumber()).isEqualTo(1);

            ItemsPageData secondPage = itemService.getItemsPage("", "NO", 2, 2, testSessionId);
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
        }
    }

    @Nested
    @DisplayName("Тесты построения сетки товаров")
    class ItemsGridBuilderTests {

        @Test
        @DisplayName("Должен создать сетку 3x3 для 9 товаров")
        void shouldCreate3x3GridFor9Items() {
            for (int i = 0; i < 4; i++) {
                Item item = new Item();
                item.setTitle("Дополнительный товар " + i);
                item.setDescription("Описание");
                item.setPrice(100L);
                itemRepository.save(item);
            }

            ItemsPageData pageData = itemService.getItemsPage("", "NO", 1, 10, testSessionId);
            List<List<ItemDto>> grid = pageData.getItemsGrid();

            assertThat(grid.size()).isGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("Должен создать сетку с заглушками для пустых ячеек")
        void shouldCreateGridWithPlaceholdersForEmptyCells() {

            ItemsPageData pageData = itemService.getItemsPage("", "NO", 1, 2, testSessionId);
            List<List<ItemDto>> grid = pageData.getItemsGrid();

            List<ItemDto> lastRow = grid.getLast();
            long placeholderCount = lastRow.stream().filter(item -> item.id() == -1).count();
            assertThat(placeholderCount).isGreaterThan(0);
        }
    }
}