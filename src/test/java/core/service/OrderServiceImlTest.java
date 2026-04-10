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
import ru.yandex.practicum.my_market_app.api.handler.CartNotFoundException;
import ru.yandex.practicum.my_market_app.core.model.OrderDto;
import ru.yandex.practicum.my_market_app.core.service.CartService;
import ru.yandex.practicum.my_market_app.core.service.OrderService;
import ru.yandex.practicum.my_market_app.persistence.entity.*;
import ru.yandex.practicum.my_market_app.persistence.repository.CartItemRepository;
import ru.yandex.practicum.my_market_app.persistence.repository.CartRepository;
import ru.yandex.practicum.my_market_app.persistence.repository.ItemRepository;
import ru.yandex.practicum.my_market_app.persistence.repository.OrderRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@SpringBootTest(classes = {Application.class, PgTestContainerConfiguration.class})
@ActiveProfiles("test")
@DisplayName("Интеграционные тесты сервиса заказов с реальной БД")
class OrderServiceImplTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderRepository orderRepository;

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

    @BeforeEach
    void setUp() {
        Long cartId = cartService.getCurrentCartId();
        testCart = cartRepository.findById(cartId).orElseThrow();

        testItem1 = new Item();
        testItem1.setTitle("Тестовый товар 1");
        testItem1.setDescription("Описание товара 1");
        testItem1.setImgPath("/images/test1.jpg");
        testItem1.setPrice(100L);
        testItem1 = itemRepository.save(testItem1);

        testItem2 = new Item();
        testItem2.setTitle("Тестовый товар 2");
        testItem2.setDescription("Описание товара 2");
        testItem2.setImgPath("/images/test2.jpg");
        testItem2.setPrice(200L);
        testItem2 = itemRepository.save(testItem2);

        testItem3 = new Item();
        testItem3.setTitle("Тестовый товар 3");
        testItem3.setDescription("Описание товара 3");
        testItem3.setImgPath("/images/test3.jpg");
        testItem3.setPrice(150L);
        testItem3 = itemRepository.save(testItem3);
    }

    @Nested
    @DisplayName("Тесты получения всех заказов")
    class GetAllOrdersTests {

        @Test
        @DisplayName("Должен вернуть пустой список, если заказов нет")
        void shouldReturnEmptyListWhenNoOrders() {
            List<OrderDto> orders = orderService.getAllOrders();

            assertThat(orders).isNotNull();
            assertThat(orders).isEmpty();
        }

        @Test
        @DisplayName("Должен вернуть список всех заказов")
        void shouldReturnAllOrders() {
            addItemsToCart(testCart.getId(), testItem1.getId(), 2);
            addItemsToCart(testCart.getId(), testItem2.getId(), 1);
            Order order1 = orderService.createOrderFromCart(testCart.getId());

            addItemsToCart(testCart.getId(), testItem3.getId(), 3);
            Order order2 = orderService.createOrderFromCart(testCart.getId());

            List<OrderDto> orders = orderService.getAllOrders();

            assertThat(orders).hasSize(2);
            assertThat(orders.get(0).id()).isEqualTo(order2.getId());
            assertThat(orders.get(1).id()).isEqualTo(order1.getId());
        }

        @Test
        @DisplayName("Должен вернуть заказы в порядке убывания даты создания")
        void shouldReturnOrdersSortedByCreatedAtDesc() throws InterruptedException {
            addItemsToCart(testCart.getId(), testItem1.getId(), 1);
            Order order1 = orderService.createOrderFromCart(testCart.getId());


            addItemsToCart(testCart.getId(), testItem2.getId(), 1);
            Order order2 = orderService.createOrderFromCart(testCart.getId());

            List<OrderDto> orders = orderService.getAllOrders();

            assertThat(orders).hasSize(2);
            assertThat(orders.get(0).id()).isEqualTo(order2.getId());
            assertThat(orders.get(1).id()).isEqualTo(order1.getId());
        }
    }

    @Nested
    @DisplayName("Тесты создания заказа из корзины")
    class CreateOrderFromCartTests {

        @Test
        @DisplayName("Должен создать заказ из корзины с товарами")
        void shouldCreateOrderFromCartWithItems() {
            addItemsToCart(testCart.getId(), testItem1.getId(), 2);
            addItemsToCart(testCart.getId(), testItem2.getId(), 1);

            Order order = orderService.createOrderFromCart(testCart.getId());

            assertThat(order).isNotNull();
            assertThat(order.getId()).isNotNull();
            assertThat(order.getOrderNumber()).isNotNull();
            assertThat(order.getOrderNumber()).startsWith("ORDER-");
            assertThat(order.getStatus()).isNotNull();
            assertThat(order.getTotalSum()).isEqualTo(100 * 2 + 200);
            assertThat(order.getItems()).hasSize(2);

            List<CartItem> cartItems = cartItemRepository.findByCartId(testCart.getId());
            assertThat(cartItems).isEmpty();
        }

        @Test
        @DisplayName("Должен создать заказ с правильными позициями")
        void shouldCreateOrderWithCorrectOrderItems() {
            addItemsToCart(testCart.getId(), testItem1.getId(), 3);
            addItemsToCart(testCart.getId(), testItem2.getId(), 2);

            Order order = orderService.createOrderFromCart(testCart.getId());

            assertThat(order.getItems()).hasSize(2);

            OrderItem orderItem1 = order.getItems().stream()
                    .filter(oi -> oi.getItem().getId().equals(testItem1.getId()))
                    .findFirst()
                    .orElse(null);
            assertThat(orderItem1).isNotNull();
            assertThat(orderItem1.getTitle()).isEqualTo(testItem1.getTitle());
            assertThat(orderItem1.getPrice()).isEqualTo(testItem1.getPrice());
            assertThat(orderItem1.getQuantity()).isEqualTo(3);

            OrderItem orderItem2 = order.getItems().stream()
                    .filter(oi -> oi.getItem().getId().equals(testItem2.getId()))
                    .findFirst()
                    .orElse(null);
            assertThat(orderItem2).isNotNull();
            assertThat(orderItem2.getTitle()).isEqualTo(testItem2.getTitle());
            assertThat(orderItem2.getPrice()).isEqualTo(testItem2.getPrice());
            assertThat(orderItem2.getQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("Должен создать заказ с правильной общей суммой")
        void shouldCreateOrderWithCorrectTotalSum() {
            addItemsToCart(testCart.getId(), testItem1.getId(), 2);
            addItemsToCart(testCart.getId(), testItem2.getId(), 3);
            addItemsToCart(testCart.getId(), testItem3.getId(), 1);
            long expectedTotal = 200 + 600 + 150;

            Order order = orderService.createOrderFromCart(testCart.getId());

            assertThat(order.getTotalSum()).isEqualTo(expectedTotal);
        }

        @Test
        @DisplayName("Должен выбросить исключение при создании заказа из пустой корзины")
        void shouldThrowExceptionWhenCreatingOrderFromEmptyCart() {
            assertThatThrownBy(() -> orderService.createOrderFromCart(testCart.getId()))
                    .isInstanceOf(CartNotFoundException.class)
                    .hasMessageContaining("Корзина не пустая");
        }

        @Test
        @DisplayName("Должен выбросить исключение при создании заказа из несуществующей корзины")
        void shouldThrowExceptionWhenCreatingOrderFromNonExistentCart() {
            assertThatThrownBy(() -> orderService.createOrderFromCart(99999L))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Должен очистить корзину после создания заказа")
        void shouldClearCartAfterCreatingOrder() {
            addItemsToCart(testCart.getId(), testItem1.getId(), 2);
            addItemsToCart(testCart.getId(), testItem2.getId(), 1);

            List<CartItem> beforeItems = cartItemRepository.findByCartId(testCart.getId());
            assertThat(beforeItems).isNotEmpty();

            orderService.createOrderFromCart(testCart.getId());

            List<CartItem> afterItems = cartItemRepository.findByCartId(testCart.getId());
            assertThat(afterItems).isEmpty();
        }

        @Test
        @DisplayName("Должен создать заказ с уникальным номером")
        void shouldCreateOrderWithUniqueNumber() {
            addItemsToCart(testCart.getId(), testItem1.getId(), 1);
            Order order1 = orderService.createOrderFromCart(testCart.getId());

            addItemsToCart(testCart.getId(), testItem2.getId(), 1);
            Order order2 = orderService.createOrderFromCart(testCart.getId());

            assertThat(order1.getOrderNumber()).isNotEqualTo(order2.getOrderNumber());
        }
    }

    @Nested
    @DisplayName("Тесты получения заказа по ID")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Должен вернуть заказ по существующему ID")
        void shouldReturnOrderById() {
            addItemsToCart(testCart.getId(), testItem1.getId(), 2);
            Order savedOrder = orderService.createOrderFromCart(testCart.getId());

            OrderDto order = orderService.getOrderById(savedOrder.getId());

            assertThat(order).isNotNull();
            assertThat(order.id()).isEqualTo(savedOrder.getId());
            assertThat(order.orderNumber()).isEqualTo(savedOrder.getOrderNumber());
            assertThat(order.totalSum()).isEqualTo(savedOrder.getTotalSum());
            assertThat(order.status()).isEqualTo(savedOrder.getStatus().name());
            assertThat(order.items()).hasSize(1);
        }

        @Test
        @DisplayName("Должен выбросить исключение при ненайденном заказе")
        void shouldThrowExceptionWhenOrderNotFound() {
            assertThatThrownBy(() -> orderService.getOrderById(99999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Заказ не найден");
        }

        @Test
        @DisplayName("Должен вернуть заказ с правильными позициями")
        void shouldReturnOrderWithCorrectItems() {
            addItemsToCart(testCart.getId(), testItem1.getId(), 2);
            addItemsToCart(testCart.getId(), testItem2.getId(), 1);
            Order savedOrder = orderService.createOrderFromCart(testCart.getId());

            OrderDto order = orderService.getOrderById(savedOrder.getId());

            assertThat(order.items()).hasSize(2);

            var item1 = order.items().stream()
                    .filter(i -> i.id().equals(testItem1.getId()))
                    .findFirst().orElse(null);
            assertThat(item1).isNotNull();
            assertThat(item1.title()).isEqualTo(testItem1.getTitle());
            assertThat(item1.price()).isEqualTo(testItem1.getPrice());
            assertThat(item1.count()).isEqualTo(2);

            var item2 = order.items().stream()
                    .filter(i -> i.id().equals(testItem2.getId()))
                    .findFirst().orElse(null);
            assertThat(item2).isNotNull();
            assertThat(item2.title()).isEqualTo(testItem2.getTitle());
            assertThat(item2.price()).isEqualTo(testItem2.getPrice());
            assertThat(item2.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Тесты генерации номера заказа")
    class OrderNumberGenerationTests {

        @Test
        @DisplayName("Должен генерировать номер заказа в правильном формате")
        void shouldGenerateOrderNumberInCorrectFormat() {
            addItemsToCart(testCart.getId(), testItem1.getId(), 1);
            Order order = orderService.createOrderFromCart(testCart.getId());

            assertThat(order.getOrderNumber()).matches("ORDER-\\d{14}-\\d{5}");
        }

        @Test
        @DisplayName("Должен генерировать уникальные номера заказов")
        void shouldGenerateUniqueOrderNumbers() {
            addItemsToCart(testCart.getId(), testItem1.getId(), 1);
            Order order1 = orderService.createOrderFromCart(testCart.getId());

            addItemsToCart(testCart.getId(), testItem2.getId(), 1);
            Order order2 = orderService.createOrderFromCart(testCart.getId());

            assertThat(order1.getOrderNumber()).isNotEqualTo(order2.getOrderNumber());
        }
    }

    @Nested
    @DisplayName("Интеграционные тесты потока данных")
    class IntegrationFlowTests {

        @Test
        @DisplayName("Должен корректно обработать полный цикл: корзина -> заказ -> получение заказа")
        void shouldHandleFullCartToOrderCycle() {
            addItemsToCart(testCart.getId(), testItem1.getId(), 2);
            addItemsToCart(testCart.getId(), testItem2.getId(), 1);

            List<CartItem> cartItems = cartItemRepository.findByCartId(testCart.getId());
            assertThat(cartItems).hasSize(2);

            Order order = orderService.createOrderFromCart(testCart.getId());
            assertThat(order).isNotNull();
            assertThat(order.getTotalSum()).isEqualTo(100 * 2 + 200);

            cartItems = cartItemRepository.findByCartId(testCart.getId());
            assertThat(cartItems).isEmpty();

            OrderDto fetchedOrder = orderService.getOrderById(order.getId());
            assertThat(fetchedOrder).isNotNull();
            assertThat(fetchedOrder.totalSum()).isEqualTo(100 * 2 + 200);
            assertThat(fetchedOrder.items()).hasSize(2);

            List<OrderDto> allOrders = orderService.getAllOrders();
            assertThat(allOrders).hasSize(1);
            assertThat(allOrders.getFirst().id()).isEqualTo(order.getId());
        }

        @Test
        @DisplayName("Должен корректно обработать несколько заказов подряд")
        void shouldHandleMultipleOrdersInRow() {
            addItemsToCart(testCart.getId(), testItem1.getId(), 1);
            Order order1 = orderService.createOrderFromCart(testCart.getId());

            addItemsToCart(testCart.getId(), testItem2.getId(), 2);
            Order order2 = orderService.createOrderFromCart(testCart.getId());

            addItemsToCart(testCart.getId(), testItem3.getId(), 3);
            Order order3 = orderService.createOrderFromCart(testCart.getId());

            List<OrderDto> allOrders = orderService.getAllOrders();
            assertThat(allOrders).hasSize(3);

            assertThat(order1.getTotalSum()).isEqualTo(100);
            assertThat(order2.getTotalSum()).isEqualTo(200 * 2);
            assertThat(order3.getTotalSum()).isEqualTo(150 * 3);
        }

        @Test
        @DisplayName("Должен корректно обработать заказ с большим количеством товаров")
        void shouldHandleOrderWithManyItems() {
            for (int i = 0; i < 10; i++) {
                addItemsToCart(testCart.getId(), testItem1.getId(), 1);
            }

            Order order = orderService.createOrderFromCart(testCart.getId());

            assertThat(order.getItems()).hasSize(1);
            assertThat(order.getItems().getFirst().getQuantity()).isEqualTo(10);
            assertThat(order.getTotalSum()).isEqualTo(100 * 10);
        }

        @Test
        @DisplayName("Должен корректно обработать создание заказа после очистки корзины")
        void shouldHandleOrderCreationAfterCartCleared() {
            addItemsToCart(testCart.getId(), testItem1.getId(), 2);

            Order order1 = orderService.createOrderFromCart(testCart.getId());
            assertThat(order1).isNotNull();

            assertThatThrownBy(() -> orderService.createOrderFromCart(testCart.getId()))
                    .isInstanceOf(CartNotFoundException.class);
        }
    }

    private void addItemsToCart(Long cartId, Long itemId, int quantity) {
        for (int i = 0; i < quantity; i++) {
            Item item = itemRepository.findById(itemId).orElseThrow();
            cartService.updateItemCount(cartId, item, "PLUS");
        }
    }
}