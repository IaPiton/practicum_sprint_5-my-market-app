package ru.yandex.practicum.my_market_app.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.my_market_app.api.handler.CartNotFoundException;
import ru.yandex.practicum.my_market_app.core.model.CartItemDto;
import ru.yandex.practicum.my_market_app.persistence.entity.Cart;
import ru.yandex.practicum.my_market_app.persistence.entity.CartItem;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;
import ru.yandex.practicum.my_market_app.persistence.repository.CartItemRepository;
import ru.yandex.practicum.my_market_app.persistence.repository.CartRepository;
import ru.yandex.practicum.my_market_app.persistence.repository.ItemRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Интеграционные тесты сервиса корзины с реальной БД")
class CartServiceImplTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ItemRepository itemRepository;

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
        testItem1.setTitle("Товар A");
        testItem1.setDescription("Описание товара A");
        testItem1.setImgPath("/images/test1.jpg");
        testItem1.setPrice(100L);
        testItem1 = itemRepository.save(testItem1);

        testItem2 = new Item();
        testItem2.setTitle("Товар B");
        testItem2.setDescription("Описание товара B");
        testItem2.setImgPath("/images/test2.jpg");
        testItem2.setPrice(200L);
        testItem2 = itemRepository.save(testItem2);

        testItem3 = new Item();
        testItem3.setTitle("Товар C");
        testItem3.setDescription("Описание товара C");
        testItem3.setImgPath("/images/test3.jpg");
        testItem3.setPrice(150L);
        testItem3 = itemRepository.save(testItem3);
    }

    @Nested
    @DisplayName("Тесты получения ID текущей корзины")
    class GetCurrentCartIdTests {

        @Test
        @DisplayName("Должен создать новую корзину, если её не существует")
        void shouldCreateNewCartWhenNotExists() {
            String newSessionId = "new-session-" + System.currentTimeMillis();
            Long cartId = cartService.getCurrentCartId(newSessionId);

            assertThat(cartId).isNotNull();
            Optional<Cart> savedCart = cartRepository.findById(cartId);
            assertThat(savedCart).isPresent();
            assertThat(savedCart.get().getSessionId()).isEqualTo(newSessionId);
        }

        @Test
        @DisplayName("Должен вернуть существующую корзину по sessionId")
        void shouldReturnExistingCartBySessionId() {
            Long cartId = cartService.getCurrentCartId(testSessionId);

            assertThat(cartId).isEqualTo(testCart.getId());
        }

        @Test
        @DisplayName("Должен создавать разные корзины для разных sessionId")
        void shouldCreateDifferentCartsForDifferentSessions() {
            String session1 = "session-1";
            String session2 = "session-2";

            Long cartId1 = cartService.getCurrentCartId(session1);
            Long cartId2 = cartService.getCurrentCartId(session2);

            assertThat(cartId1).isNotEqualTo(cartId2);

            Optional<Cart> cart1 = cartRepository.findById(cartId1);
            Optional<Cart> cart2 = cartRepository.findById(cartId2);

            assertThat(cart1).isPresent();
            assertThat(cart2).isPresent();
            assertThat(cart1.get().getSessionId()).isEqualTo(session1);
            assertThat(cart2.get().getSessionId()).isEqualTo(session2);
        }
    }

    @Nested
    @DisplayName("Тесты получения количества товаров в корзине")
    class GetItemCountsTests {

        @Test
        @DisplayName("Должен вернуть пустую карту для пустой корзины")
        void shouldReturnEmptyMapForEmptyCart() {
            Map<Long, Integer> counts = cartService.getItemCounts(testCart.getId());

            assertThat(counts).isNotNull();
            assertThat(counts).isEmpty();
        }

        @Test
        @DisplayName("Должен вернуть правильное количество товаров в корзине")
        void shouldReturnCorrectItemCounts() {
            addItemToCart(testCart.getId(), testItem1.getId(), 2);
            addItemToCart(testCart.getId(), testItem2.getId(), 3);

            Map<Long, Integer> counts = cartService.getItemCounts(testCart.getId());

            assertThat(counts).hasSize(2);
            assertThat(counts.get(testItem1.getId())).isEqualTo(2);
            assertThat(counts.get(testItem2.getId())).isEqualTo(3);
        }

        @Test
        @DisplayName("Должен вернуть пустую карту для несуществующей корзины")
        void shouldReturnEmptyMapForNonExistentCart() {
            Map<Long, Integer> counts = cartService.getItemCounts(99999L);

            assertThat(counts).isNotNull();
            assertThat(counts).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты обновления количества товара в корзине")
    class UpdateItemCountTests {

        @Test
        @DisplayName("Должен добавить новый товар в корзину при действии PLUS")
        void shouldAddNewItemToCartWhenPlusAction() {
            cartService.updateItemCount(testCart.getId(), testItem1, "PLUS");

            List<CartItem> cartItems = cartItemRepository.findByCartId(testCart.getId());
            assertThat(cartItems).hasSize(1);
            assertThat(cartItems.getFirst().getItem().getId()).isEqualTo(testItem1.getId());
            assertThat(cartItems.getFirst().getQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("Должен увеличить количество существующего товара при действии PLUS")
        void shouldIncreaseExistingItemQuantityWhenPlusAction() {
            addItemToCart(testCart.getId(), testItem1.getId(), 2);

            cartService.updateItemCount(testCart.getId(), testItem1, "PLUS");

            Optional<CartItem> cartItem = cartItemRepository.findByCartIdAndItemId(testCart.getId(), testItem1.getId());
            assertThat(cartItem).isPresent();
            assertThat(cartItem.get().getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("Должен уменьшить количество товара при действии MINUS")
        void shouldDecreaseItemQuantityWhenMinusAction() {
            addItemToCart(testCart.getId(), testItem1.getId(), 3);

            cartService.updateItemCount(testCart.getId(), testItem1, "MINUS");

            Optional<CartItem> cartItem = cartItemRepository.findByCartIdAndItemId(testCart.getId(), testItem1.getId());
            assertThat(cartItem).isPresent();
            assertThat(cartItem.get().getQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("Должен удалить товар из корзины при уменьшении до 0")
        void shouldRemoveItemWhenQuantityBecomesZero() {
            addItemToCart(testCart.getId(), testItem1.getId(), 1);

            cartService.updateItemCount(testCart.getId(), testItem1, "MINUS");

            Optional<CartItem> cartItem = cartItemRepository.findByCartIdAndItemId(testCart.getId(), testItem1.getId());
            assertThat(cartItem).isNotPresent();
        }

        @Test
        @DisplayName("Должен удалить товар из корзины при действии DELETE")
        void shouldRemoveItemWhenDeleteAction() {
            addItemToCart(testCart.getId(), testItem1.getId(), 5);

            cartService.updateItemCount(testCart.getId(), testItem1, "DELETE");

            Optional<CartItem> cartItem = cartItemRepository.findByCartIdAndItemId(testCart.getId(), testItem1.getId());
            assertThat(cartItem).isNotPresent();
        }

        @Test
        @DisplayName("Должен ничего не делать при MINUS для отсутствующего товара")
        void shouldDoNothingWhenMinusOnNonExistentItem() {
            cartService.updateItemCount(testCart.getId(), testItem1, "MINUS");

            List<CartItem> cartItems = cartItemRepository.findByCartId(testCart.getId());
            assertThat(cartItems).isEmpty();
        }

        @Test
        @DisplayName("Должен выбросить исключение при неверном действии")
        void shouldThrowExceptionWhenInvalidAction() {
            assertThatThrownBy(() -> cartService.updateItemCount(testCart.getId(), testItem1, "INVALID"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Неизвестное действие");
        }

        @Test
        @DisplayName("Должен выбросить исключение при ненайденной корзине")
        void shouldThrowExceptionWhenCartNotFound() {
            assertThatThrownBy(() -> cartService.updateItemCount(99999L, testItem1, "PLUS"))
                    .isInstanceOf(CartNotFoundException.class)
                    .hasMessageContaining("Корзина не найдена");
        }
    }

    @Nested
    @DisplayName("Тесты получения товаров в корзине с деталями")
    class GetCartItemsWithDetailsTests {

        @Test
        @DisplayName("Должен вернуть пустой список для пустой корзины")
        void shouldReturnEmptyListForEmptyCart() {
            List<CartItemDto> items = cartService.getCartItemsWithDetails(testCart.getId());

            assertThat(items).isNotNull();
            assertThat(items).isEmpty();
        }

        @Test
        @DisplayName("Должен вернуть список товаров с правильными деталями")
        void shouldReturnItemsWithCorrectDetails() {
            addItemToCart(testCart.getId(), testItem1.getId(), 2);
            addItemToCart(testCart.getId(), testItem2.getId(), 1);

            List<CartItemDto> items = cartService.getCartItemsWithDetails(testCart.getId());

            assertThat(items).hasSize(2);

            CartItemDto item1 = items.stream()
                    .filter(i -> i.id().equals(testItem1.getId()))
                    .findFirst().orElse(null);
            assertThat(item1).isNotNull();
            assertThat(item1.title()).isEqualTo(testItem1.getTitle());
            assertThat(item1.price()).isEqualTo(testItem1.getPrice());
            assertThat(item1.count()).isEqualTo(2);
            assertThat(item1.subtotal()).isEqualTo(200L);

            CartItemDto item2 = items.stream()
                    .filter(i -> i.id().equals(testItem2.getId()))
                    .findFirst().orElse(null);
            assertThat(item2).isNotNull();
            assertThat(item2.title()).isEqualTo(testItem2.getTitle());
            assertThat(item2.price()).isEqualTo(testItem2.getPrice());
            assertThat(item2.count()).isEqualTo(1);
            assertThat(item2.subtotal()).isEqualTo(200L);
        }

        @Test
        @DisplayName("Должен вернуть отсортированные по названию товары (A-Z)")
        void shouldReturnItemsSortedByTitleAscending() {
            addItemToCart(testCart.getId(), testItem2.getId(), 1); // Товар B
            addItemToCart(testCart.getId(), testItem1.getId(), 2); // Товар A
            addItemToCart(testCart.getId(), testItem3.getId(), 3); // Товар C

            List<CartItemDto> items = cartService.getCartItemsWithDetails(testCart.getId());

            assertThat(items).hasSize(3);
            assertThat(items.get(0).title()).isEqualTo(testItem3.getTitle()); // C
            assertThat(items.get(1).title()).isEqualTo(testItem2.getTitle()); // B
            assertThat(items.get(2).title()).isEqualTo(testItem1.getTitle()); // A
        }

        @Test
        @DisplayName("Должен вернуть пустой список для несуществующей корзины")
        void shouldReturnEmptyListForNonExistentCart() {
            List<CartItemDto> items = cartService.getCartItemsWithDetails(99999L);

            assertThat(items).isNotNull();
            assertThat(items).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты получения общей суммы корзины")
    class GetCartTotalTests {

        @Test
        @DisplayName("Должен вернуть 0 для пустой корзины")
        void shouldReturnZeroForEmptyCart() {
            Long total = cartService.getCartTotal(testCart.getId());

            assertThat(total).isZero();
        }

        @Test
        @DisplayName("Должен вернуть правильную сумму для корзины с товарами")
        void shouldReturnCorrectTotalForCartWithItems() {
            addItemToCart(testCart.getId(), testItem1.getId(), 2); // 100 * 2 = 200
            addItemToCart(testCart.getId(), testItem2.getId(), 3); // 200 * 3 = 600
            addItemToCart(testCart.getId(), testItem3.getId(), 1); // 150 * 1 = 150

            Long total = cartService.getCartTotal(testCart.getId());

            assertThat(total).isEqualTo(200 + 600 + 150);
        }

        @Test
        @DisplayName("Должен вернуть 0 для несуществующей корзины")
        void shouldReturnZeroForNonExistentCart() {
            Long total = cartService.getCartTotal(99999L);

            assertThat(total).isZero();
        }

        @Test
        @DisplayName("Сумма должна обновляться после изменений в корзине")
        void shouldUpdateTotalAfterCartChanges() {
            addItemToCart(testCart.getId(), testItem1.getId(), 2);
            Long initialTotal = cartService.getCartTotal(testCart.getId());
            assertThat(initialTotal).isEqualTo(200);

            cartService.updateItemCount(testCart.getId(), testItem1, "PLUS");
            Long afterPlusTotal = cartService.getCartTotal(testCart.getId());
            assertThat(afterPlusTotal).isEqualTo(300);

            cartService.updateItemCount(testCart.getId(), testItem1, "DELETE");
            Long afterDeleteTotal = cartService.getCartTotal(testCart.getId());
            assertThat(afterDeleteTotal).isZero();
        }
    }

    @Nested
    @DisplayName("Интеграционные тесты потока данных")
    class IntegrationFlowTests {

        @Test
        @DisplayName("Должен корректно обработать полный цикл операций с корзиной")
        void shouldHandleFullCartCycle() {
            cartService.updateItemCount(testCart.getId(), testItem1, "PLUS");
            cartService.updateItemCount(testCart.getId(), testItem1, "PLUS");
            cartService.updateItemCount(testCart.getId(), testItem2, "PLUS");

            Map<Long, Integer> counts = cartService.getItemCounts(testCart.getId());
            assertThat(counts.get(testItem1.getId())).isEqualTo(2);
            assertThat(counts.get(testItem2.getId())).isEqualTo(1);

            Long total = cartService.getCartTotal(testCart.getId());
            assertThat(total).isEqualTo(100 * 2 + 200);

            cartService.updateItemCount(testCart.getId(), testItem1, "MINUS");
            counts = cartService.getItemCounts(testCart.getId());
            assertThat(counts.get(testItem1.getId())).isEqualTo(1);

            total = cartService.getCartTotal(testCart.getId());
            assertThat(total).isEqualTo(100 + 200);

            cartService.updateItemCount(testCart.getId(), testItem2, "DELETE");
            counts = cartService.getItemCounts(testCart.getId());
            assertThat(counts).doesNotContainKey(testItem2.getId());

            total = cartService.getCartTotal(testCart.getId());
            assertThat(total).isEqualTo(100);
        }

        @Test
        @DisplayName("Должен корректно обработать множество операций с разными товарами")
        void shouldHandleMultipleOperationsWithDifferentItems() {
            for (int i = 0; i < 5; i++) {
                cartService.updateItemCount(testCart.getId(), testItem1, "PLUS");
            }
            for (int i = 0; i < 3; i++) {
                cartService.updateItemCount(testCart.getId(), testItem2, "PLUS");
            }
            for (int i = 0; i < 2; i++) {
                cartService.updateItemCount(testCart.getId(), testItem3, "PLUS");
            }

            Map<Long, Integer> counts = cartService.getItemCounts(testCart.getId());
            assertThat(counts.get(testItem1.getId())).isEqualTo(5);
            assertThat(counts.get(testItem2.getId())).isEqualTo(3);
            assertThat(counts.get(testItem3.getId())).isEqualTo(2);

            Long total = cartService.getCartTotal(testCart.getId());
            assertThat(total).isEqualTo(100*5 + 200*3 + 150*2);

            cartService.updateItemCount(testCart.getId(), testItem1, "DELETE");
            cartService.updateItemCount(testCart.getId(), testItem2, "DELETE");
            cartService.updateItemCount(testCart.getId(), testItem3, "DELETE");

            List<CartItemDto> items = cartService.getCartItemsWithDetails(testCart.getId());
            assertThat(items).isEmpty();
            assertThat(cartService.getCartTotal(testCart.getId())).isZero();
        }

        @Test
        @DisplayName("Должен корректно обработать операции с несуществующей корзиной")
        void shouldHandleOperationsWithNonExistentCart() {
            Long nonExistentCartId = 99999L;

            assertThatThrownBy(() -> cartService.updateItemCount(nonExistentCartId, testItem1, "PLUS"))
                    .isInstanceOf(CartNotFoundException.class);

            Map<Long, Integer> counts = cartService.getItemCounts(nonExistentCartId);
            assertThat(counts).isEmpty();

            List<CartItemDto> items = cartService.getCartItemsWithDetails(nonExistentCartId);
            assertThat(items).isEmpty();

            Long total = cartService.getCartTotal(nonExistentCartId);
            assertThat(total).isZero();
        }

        @Test
        @DisplayName("Должен корректно обработать DELETE для отсутствующего товара")
        void shouldHandleDeleteForNonExistentItem() {
            cartService.updateItemCount(testCart.getId(), testItem1, "DELETE");

            List<CartItem> cartItems = cartItemRepository.findByCartId(testCart.getId());
            assertThat(cartItems).isEmpty();
        }
    }

    private void addItemToCart(Long cartId, Long itemId, int quantity) {
        for (int i = 0; i < quantity; i++) {
            Item item = itemRepository.findById(itemId).orElseThrow();
            cartService.updateItemCount(cartId, item, "PLUS");
        }
    }
}