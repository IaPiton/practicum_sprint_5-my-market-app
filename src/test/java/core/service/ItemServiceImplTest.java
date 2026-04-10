package core.service;

import configuration.PgTestContainerConfiguration;
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
import ru.yandex.practicum.my_market_app.core.service.CartService;
import ru.yandex.practicum.my_market_app.core.service.ItemService;
import ru.yandex.practicum.my_market_app.persistence.entity.Cart;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;
import ru.yandex.practicum.my_market_app.persistence.model.ItemDto;
import ru.yandex.practicum.my_market_app.persistence.repository.CartRepository;
import ru.yandex.practicum.my_market_app.persistence.repository.ItemRepository;

import java.util.List;

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

    @BeforeEach
    void setUp() {
        testCart = new Cart();
        testCart.setSessionId("test-session-" + System.currentTimeMillis());
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

        Item testItem4 = new Item();
        testItem4.setTitle("Виноград");
        testItem4.setDescription("Сладкий виноград");
        testItem4.setImgPath("/images/grape.jpg");
        testItem4.setPrice(300L);
        itemRepository.save(testItem4);

        Item testItem5 = new Item();
        testItem5.setTitle("Груша");
        testItem5.setDescription("Сочная груша");
        testItem5.setImgPath("/images/pear.jpg");
        testItem5.setPrice(250L);
        itemRepository.save(testItem5);
    }

    @Nested
    @DisplayName("Тесты получения страницы товаров")
    class GetItemsPageTests {

        @Test
        @DisplayName("Должен вернуть страницу товаров без поиска и сортировки")
        void shouldReturnItemsPageWithoutSearchAndSort() {
            ItemsPageData pageData = itemService.getItemsPage("", "NO", 1, 10);

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
            ItemsPageData pageData = itemService.getItemsPage("", "ALPHA", 1, 10);

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
            assertThat(items.get(2).title()).isEqualTo("Виноград");
            assertThat(items.get(3).title()).isEqualTo("Груша");
            assertThat(items.get(4).title()).isEqualTo("Яблоко");
        }

        @Test
        @DisplayName("Должен вернуть страницу товаров с сортировкой по цене PRICE")
        void shouldReturnItemsPageWithPriceSort() {
            ItemsPageData pageData = itemService.getItemsPage("", "PRICE", 1, 10);

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
            assertThat(items.get(3).price()).isEqualTo(250L);
            assertThat(items.get(4).price()).isEqualTo(300L);
        }

        @Test
        @DisplayName("Должен вернуть страницу товаров с поиском")
        void shouldReturnItemsPageWithSearch() {
            ItemsPageData pageData = itemService.getItemsPage("яблоко", "NO", 1, 10);

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
            ItemsPageData pageData = itemService.getItemsPage("несуществующий товар", "NO", 1, 10);

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
            ItemsPageData pageData = itemService.getItemsPage("", "NO", 1, 10);

            List<List<ItemDto>> grid = pageData.getItemsGrid();
            assertThat(grid).isNotEmpty();

            for (List<ItemDto> row : grid) {
                assertThat(row.size()).isLessThanOrEqualTo(3);
            }
        }

        @Test
        @DisplayName("Должен добавить заглушки для пустых ячеек в последней строке")
        void shouldAddPlaceholdersForEmptyCells() {
            ItemsPageData pageData = itemService.getItemsPage("", "NO", 1, 4);

            List<List<ItemDto>> grid = pageData.getItemsGrid();

            assertThat(grid).hasSize(2);

            List<ItemDto> lastRow = grid.get(1);
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
            ItemDto item = itemService.getItemById(testItem1.getId());

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
            assertThatThrownBy(() -> itemService.getItemById(99999L))
                    .isInstanceOf(ItemNotFoundException.class)
                    .hasMessageContaining("Товар не найден");
        }

        @Test
        @DisplayName("Должен вернуть товар с количеством в корзине")
        void shouldReturnItemWithCartCount() {
            Long currentCartId = cartService.getCurrentCartId();

            cartService.updateItemCount(currentCartId, testItem1, "PLUS");
            cartService.updateItemCount(currentCartId, testItem1, "PLUS");

            ItemDto item = itemService.getItemById(testItem1.getId());

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
                    testItem1.getId(), "тест", "ALPHA", 2, 10, "PLUS");

            assertThat(redirectUrl).isNotNull();
            assertThat(redirectUrl).contains("/items?search=тест&sort=ALPHA&pageNumber=2&pageSize=10");

            Long cartId = cartService.getCurrentCartId();
            int count = cartService.getItemCounts(cartId).getOrDefault(testItem1.getId(), 0);
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Должен уменьшить количество товара и вернуть URL для редиректа при MINUS")
        void shouldDecreaseItemCountAndReturnRedirectUrlWhenMinusAction() {
            Long currentCartId = cartService.getCurrentCartId();

            cartService.updateItemCount(currentCartId, testItem1, "PLUS");
            cartService.updateItemCount(currentCartId, testItem1, "PLUS");

            String redirectUrl = itemService.updateCartItemAndGetRedirectUrl(
                    testItem1.getId(), "", "NO", 1, 5, "MINUS");

            assertThat(redirectUrl).isNotNull();
            assertThat(redirectUrl).contains("/items?sort=NO&pageNumber=1&pageSize=5");

            Long cartId = cartService.getCurrentCartId();
            int count = cartService.getItemCounts(cartId).getOrDefault(testItem1.getId(), 0);
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Должен удалить товар и вернуть URL для редиректа при DELETE")
        void shouldDeleteItemAndReturnRedirectUrlWhenDeleteAction() {
            cartService.updateItemCount(testCart.getId(), testItem1, "PLUS");

            String redirectUrl = itemService.updateCartItemAndGetRedirectUrl(
                    testItem1.getId(), "", "NO", 1, 5, "DELETE");

            assertThat(redirectUrl).isNotNull();

            Long cartId = cartService.getCurrentCartId();
            int count = cartService.getItemCounts(cartId).getOrDefault(testItem1.getId(), 0);
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Должен выбросить исключение при обновлении несуществующего товара")
        void shouldThrowExceptionWhenUpdatingNonExistentItem() {
            assertThatThrownBy(() -> itemService.updateCartItemAndGetRedirectUrl(
                    99999L, "", "NO", 1, 5, "PLUS"))
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
            ItemDto updatedItem = itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS");

            assertThat(updatedItem).isNotNull();
            assertThat(updatedItem.id()).isEqualTo(testItem1.getId());
            assertThat(updatedItem.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Должен уменьшить количество и вернуть обновленный товар при MINUS")
        void shouldDecreaseCountAndReturnUpdatedItemWhenMinusAction() {
            itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS");
            itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS");

            ItemDto updatedItem = itemService.updateItemCountAndGetItem(testItem1.getId(), "MINUS");

            assertThat(updatedItem).isNotNull();
            assertThat(updatedItem.id()).isEqualTo(testItem1.getId());
            assertThat(updatedItem.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Должен удалить товар и вернуть товар с нулевым количеством при DELETE")
        void shouldDeleteAndReturnItemWithZeroCountWhenDeleteAction() {
            itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS");

            ItemDto updatedItem = itemService.updateItemCountAndGetItem(testItem1.getId(), "DELETE");

            assertThat(updatedItem).isNotNull();
            assertThat(updatedItem.id()).isEqualTo(testItem1.getId());
            assertThat(updatedItem.count()).isZero();
        }

        @Test
        @DisplayName("Должен выбросить исключение при обновлении несуществующего товара")
        void shouldThrowExceptionWhenUpdatingNonExistentItem() {
            assertThatThrownBy(() -> itemService.updateItemCountAndGetItem(99999L, "PLUS"))
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
    @DisplayName("Интеграционные тесты потока данных")
    class IntegrationFlowTests {

        @Test
        @DisplayName("Должен корректно обработать полный цикл операций с товарами и корзиной")
        void shouldHandleFullItemAndCartCycle() {
            ItemsPageData pageData = itemService.getItemsPage("", "NO", 1, 10);
            assertThat(pageData.getItemsGrid()).isNotEmpty();

            ItemDto updatedItem = itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS");
            assertThat(updatedItem.count()).isEqualTo(1);

            ItemsPageData updatedPageData = itemService.getItemsPage("", "NO", 1, 10);
            List<List<ItemDto>> grid = updatedPageData.getItemsGrid();
            List<ItemDto> items = grid.stream()
                    .flatMap(List::stream)
                    .filter(item -> item.id().equals(testItem1.getId()))
                    .toList();
            assertThat(items.getFirst().count()).isEqualTo(1);

            updatedItem = itemService.updateItemCountAndGetItem(testItem1.getId(), "PLUS");
            assertThat(updatedItem.count()).isEqualTo(2);

            updatedItem = itemService.updateItemCountAndGetItem(testItem1.getId(), "DELETE");
            assertThat(updatedItem.count()).isZero();
        }

        @Test
        @DisplayName("Должен корректно обработать поиск и сортировку вместе")
        void shouldHandleSearchAndSortTogether() {
            Long currentCartId = cartService.getCurrentCartId();

            cartService.updateItemCount(currentCartId, testItem2, "PLUS");
            cartService.updateItemCount(currentCartId, testItem3, "PLUS");
            cartService.updateItemCount(currentCartId, testItem3, "PLUS");

            ItemsPageData pageData = itemService.getItemsPage("яблоко", "PRICE", 1, 10);
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
            ItemsPageData firstPage = itemService.getItemsPage("", "NO", 1, 2);
            assertThat(firstPage.getPaging().pageNumber()).isEqualTo(1);

            ItemsPageData secondPage = itemService.getItemsPage("", "NO", 2, 2);
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

            ItemsPageData pageData = itemService.getItemsPage("", "NO", 1, 10);
            List<List<ItemDto>> grid = pageData.getItemsGrid();

            assertThat(grid.size()).isGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("Должен создать сетку с заглушками для пустых ячеек")
        void shouldCreateGridWithPlaceholdersForEmptyCells() {
            ItemsPageData pageData = itemService.getItemsPage("", "NO", 1, 4);
            List<List<ItemDto>> grid = pageData.getItemsGrid();

            List<ItemDto> lastRow = grid.getLast();
            long placeholderCount = lastRow.stream().filter(item -> item.id() == -1).count();
            assertThat(placeholderCount).isGreaterThan(0);
        }
    }
}