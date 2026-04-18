package ru.yandex.practicum.my_market_service.core.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.my_market_service.api.handler.OrderItemNotFoundException;
import ru.yandex.practicum.my_market_service.configuration.TestcontainersTest;
import ru.yandex.practicum.my_market_service.core.model.OrderDto;
import ru.yandex.practicum.my_market_service.core.model.OrderItemDto;
import ru.yandex.practicum.my_market_service.core.security.SecurityService;
import ru.yandex.practicum.my_market_service.persistence.entity.Cart;
import ru.yandex.practicum.my_market_service.persistence.entity.Item;
import ru.yandex.practicum.my_market_service.persistence.entity.Order;
import ru.yandex.practicum.my_market_service.persistence.model.OrderStatus;
import ru.yandex.practicum.my_market_service.persistence.repository.*;
import yandex.practicum.market.client.api.PaymentApi;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Интеграционные тесты сервиса заказов с реальной БД и кэшированием")
class OrderServiceImplTest extends TestcontainersTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private PaymentApi paymentApi;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private CacheManager cacheManager;

    private Cart testCart;
    private Item testItem1;
    private Item testItem2;
    private Item testItem3;
    private final Long userId = 3L;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames()
                .forEach(cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());

        orderItemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
        cartItemRepository.deleteAll().block();
        cartRepository.deleteAll().block();
        itemRepository.deleteAll().block();



        testCart = new Cart();
        testCart.setUserId(userId);
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
        orderItemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
        cartItemRepository.deleteAll().block();
        cartRepository.deleteAll().block();
        itemRepository.deleteAll().block();

        cacheManager.getCacheNames()
                .forEach(cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
    }

    private Mono<Void> addItemToCart(Long cartId, Long itemId, int quantity) {
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new RuntimeException("Item not found with id: " + itemId)))
                .flatMap(item ->
                        Flux.range(1, quantity)
                                .concatMap(i -> cartService.updateItemCount(cartId, item, "PLUS"))
                                .then()
                );
    }

    @Nested
    @DisplayName("Тесты создания заказа из корзины")
    class CreateOrderFromCartTests {

        @Test
        @DisplayName("Должен успешно создать заказ из корзины с товарами")
        void shouldSuccessfullyCreateOrderFromCartWithItems() {
            addItemToCart(testCart.getId(), testItem1.getId(), 2).block();
            addItemToCart(testCart.getId(), testItem2.getId(), 1).block();
            addItemToCart(testCart.getId(), testItem3.getId(), 3).block();

            StepVerifier.create(orderService.createOrderFromCart(testCart.getId()))
                    .assertNext(order -> {
                        assertThat(order).isNotNull();
                        assertThat(order.getId()).isNotNull();
                        assertThat(order.getOrderNumber()).isNotNull();
                        assertThat(order.getOrderNumber()).startsWith("ORDER-");
                        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
                        assertThat(order.getCreatedAt()).isNotNull();
                        assertThat(order.getUpdatedAt()).isNotNull();
                        assertThat(order.getTotalSum()).isEqualTo(850L);
                    })
                    .verifyComplete();

            StepVerifier.create(cartItemRepository.findByCartId(testCart.getId()).collectList())
                    .assertNext(items -> assertThat(items).isEmpty())
                    .verifyComplete();

            StepVerifier.create(orderRepository.findAll().collectList())
                    .assertNext(orders -> assertThat(orders).hasSize(1))
                    .verifyComplete();

            StepVerifier.create(orderItemRepository.findAll().collectList())
                    .assertNext(orderItems -> {
                        assertThat(orderItems).hasSize(3);

                        var orderItem1 = orderItems.stream()
                                .filter(oi -> oi.getItemId().equals(testItem1.getId()))
                                .findFirst().orElse(null);
                        assertThat(orderItem1).isNotNull();
                        assertThat(orderItem1.getQuantity()).isEqualTo(2);
                        assertThat(orderItem1.getPrice()).isEqualTo(100L);
                        assertThat(orderItem1.getTitle()).isEqualTo("Апельсин");

                        var orderItem2 = orderItems.stream()
                                .filter(oi -> oi.getItemId().equals(testItem2.getId()))
                                .findFirst().orElse(null);
                        assertThat(orderItem2).isNotNull();
                        assertThat(orderItem2.getQuantity()).isEqualTo(1);
                        assertThat(orderItem2.getPrice()).isEqualTo(200L);
                        assertThat(orderItem2.getTitle()).isEqualTo("Банан");

                        var orderItem3 = orderItems.stream()
                                .filter(oi -> oi.getItemId().equals(testItem3.getId()))
                                .findFirst().orElse(null);
                        assertThat(orderItem3).isNotNull();
                        assertThat(orderItem3.getQuantity()).isEqualTo(3);
                        assertThat(orderItem3.getPrice()).isEqualTo(150L);
                        assertThat(orderItem3.getTitle()).isEqualTo("Яблоко");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен выбросить исключение при создании заказа из пустой корзины")
        void shouldThrowExceptionWhenCreatingOrderFromEmptyCart() {
            StepVerifier.create(orderService.createOrderFromCart(testCart.getId()))
                    .expectErrorMatches(throwable -> throwable instanceof OrderItemNotFoundException &&
                            throwable.getMessage().contains("В корзине нет товаров для оформления заказа"))
                    .verify();
        }

        @Test
        @DisplayName("Должен выбросить исключение при создании заказа из несуществующей корзины")
        void shouldThrowExceptionWhenCreatingOrderFromNonExistentCart() {
            StepVerifier.create(orderService.createOrderFromCart(99999L))
                    .expectErrorMatches(throwable -> throwable instanceof OrderItemNotFoundException &&
                            throwable.getMessage().contains("В корзине нет товаров для оформления заказа"))
                    .verify();
        }

        @Test
        @DisplayName("Должен корректно обработать создание нескольких заказов подряд")
        void shouldHandleMultipleOrdersCreation() {
            addItemToCart(testCart.getId(), testItem1.getId(), 1).block();

            Order firstOrder = orderService.createOrderFromCart(testCart.getId()).block();
            assertThat(firstOrder).isNotNull();
            assertThat(firstOrder.getTotalSum()).isEqualTo(100L);

            addItemToCart(testCart.getId(), testItem2.getId(), 2).block();
            addItemToCart(testCart.getId(), testItem3.getId(), 1).block();

            Order secondOrder = orderService.createOrderFromCart(testCart.getId()).block();
            assertThat(secondOrder).isNotNull();
            assertThat(secondOrder.getTotalSum()).isEqualTo(550L);

            StepVerifier.create(orderRepository.findAll().collectList())
                    .assertNext(orders -> assertThat(orders).hasSize(2))
                    .verifyComplete();

            assertThat(firstOrder.getOrderNumber()).isNotEqualTo(secondOrder.getOrderNumber());
        }
    }

    @Nested
    @DisplayName("Тесты получения всех заказов с кэшированием")
    class GetAllOrdersTests {

        @Test
        @DisplayName("Должен вернуть пустой список, если заказов нет")
        void shouldReturnEmptyListWhenNoOrders() {
            StepVerifier.create(orderService.getAllOrders(userId).collectList())
                    .assertNext(orders -> {
                        assertThat(orders).isNotNull();
                        assertThat(orders).isEmpty();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен кэшировать результат getAllOrders")
        void shouldCacheGetAllOrdersResult() {
            addItemToCart(testCart.getId(), testItem1.getId(), 2).block();
            orderService.createOrderFromCart(testCart.getId()).block();

            addItemToCart(testCart.getId(), testItem2.getId(), 1).block();
            addItemToCart(testCart.getId(), testItem3.getId(), 3).block();
            orderService.createOrderFromCart(testCart.getId()).block();

            StepVerifier.create(orderService.getAllOrders(userId).collectList())
                    .assertNext(orders -> assertThat(orders).hasSize(2))
                    .verifyComplete();

            assertThat(cacheManager.getCache("allOrders")).isNotNull();

            StepVerifier.create(orderService.getAllOrders(userId).collectList())
                    .assertNext(orders -> assertThat(orders).hasSize(2))
                    .verifyComplete();

            Objects.requireNonNull(cacheManager.getCache("allOrders")).clear();

            StepVerifier.create(orderService.getAllOrders(userId).collectList())
                    .assertNext(orders -> assertThat(orders).hasSize(2))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен вернуть все заказы с их товарами")
        void shouldReturnAllOrdersWithItems() {
            addItemToCart(testCart.getId(), testItem1.getId(), 2).block();
            Order firstOrder = orderService.createOrderFromCart(testCart.getId()).block();

            addItemToCart(testCart.getId(), testItem2.getId(), 1).block();
            addItemToCart(testCart.getId(), testItem3.getId(), 3).block();
            Order secondOrder = orderService.createOrderFromCart(testCart.getId()).block();

            StepVerifier.create(orderService.getAllOrders(userId).collectList())
                    .assertNext(orders -> {
                        assertThat(orders).hasSize(2);

                        OrderDto firstOrderDto = orders.stream()
                                .filter(o -> o.id().equals(Objects.requireNonNull(firstOrder).getId()))
                                .findFirst().orElse(null);
                        assertThat(firstOrderDto).isNotNull();
                        assertThat(firstOrderDto.orderNumber()).isEqualTo(firstOrder.getOrderNumber());
                        assertThat(firstOrderDto.status()).isEqualTo(OrderStatus.NEW.name());
                        assertThat(firstOrderDto.totalSum()).isEqualTo(200L);
                        assertThat(firstOrderDto.items()).hasSize(1);
                        assertThat(firstOrderDto.items().getFirst().title()).isEqualTo("Апельсин");
                        assertThat(firstOrderDto.items().getFirst().count()).isEqualTo(2);
                        assertThat(firstOrderDto.items().getFirst().subtotal()).isEqualTo(200L);

                        OrderDto secondOrderDto = orders.stream()
                                .filter(o -> o.id().equals(Objects.requireNonNull(secondOrder).getId()))
                                .findFirst().orElse(null);
                        assertThat(secondOrderDto).isNotNull();
                        assertThat(secondOrderDto.orderNumber()).isEqualTo(secondOrder.getOrderNumber());
                        assertThat(secondOrderDto.totalSum()).isEqualTo(650L);
                        assertThat(secondOrderDto.items()).hasSize(2);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Тесты получения заказа по ID с кэшированием")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Должен вернуть заказ по существующему ID")
        void shouldReturnOrderById() {
            addItemToCart(testCart.getId(), testItem1.getId(), 2).block();
            addItemToCart(testCart.getId(), testItem2.getId(), 1).block();
            Order createdOrder = orderService.createOrderFromCart(testCart.getId()).block();

            StepVerifier.create(orderService.getOrderById(Objects.requireNonNull(createdOrder).getId()))
                    .assertNext(orderDto -> {
                        assertThat(orderDto).isNotNull();
                        assertThat(orderDto.id()).isEqualTo(createdOrder.getId());
                        assertThat(orderDto.orderNumber()).isEqualTo(createdOrder.getOrderNumber());
                        assertThat(orderDto.status()).isEqualTo(OrderStatus.NEW.name());
                        assertThat(orderDto.totalSum()).isEqualTo(400L);
                        assertThat(orderDto.createdAt()).isNotNull();
                        assertThat(orderDto.items()).hasSize(2);

                        OrderItemDto item1 = orderDto.items().stream()
                                .filter(i -> i.id().equals(testItem1.getId()))
                                .findFirst().orElse(null);
                        assertThat(item1).isNotNull();
                        assertThat(item1.title()).isEqualTo("Апельсин");
                        assertThat(item1.count()).isEqualTo(2);
                        assertThat(item1.price()).isEqualTo(100L);
                        assertThat(item1.subtotal()).isEqualTo(200L);

                        OrderItemDto item2 = orderDto.items().stream()
                                .filter(i -> i.id().equals(testItem2.getId()))
                                .findFirst().orElse(null);
                        assertThat(item2).isNotNull();
                        assertThat(item2.title()).isEqualTo("Банан");
                        assertThat(item2.count()).isEqualTo(1);
                        assertThat(item2.price()).isEqualTo(200L);
                        assertThat(item2.subtotal()).isEqualTo(200L);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен кэшировать результат getOrderById")
        void shouldCacheGetOrderByIdResult() {
            addItemToCart(testCart.getId(), testItem1.getId(), 2).block();
            Order createdOrder = orderService.createOrderFromCart(testCart.getId()).block();
            Long orderId = Objects.requireNonNull(createdOrder).getId();

            StepVerifier.create(orderService.getOrderById(orderId))
                    .expectNextCount(1)
                    .verifyComplete();

            assertThat(cacheManager.getCache("orders")).isNotNull();
            assertThat(Objects.requireNonNull(cacheManager.getCache("orders")).get(orderId)).isNotNull();

            StepVerifier.create(orderService.getOrderById(orderId))
                    .expectNextCount(1)
                    .verifyComplete();

            Objects.requireNonNull(cacheManager.getCache("orders")).evict(orderId);

            StepVerifier.create(orderService.getOrderById(orderId))
                    .expectNextCount(1)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Должен выбросить исключение при запросе несуществующего заказа")
        void shouldThrowExceptionWhenOrderNotFound() {
            StepVerifier.create(orderService.getOrderById(99999L))
                    .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                            throwable.getMessage().contains("Заказ не найден"))
                    .verify();
        }

        @Test
        @DisplayName("Должен вернуть заказ с правильными деталями товаров")
        void shouldReturnOrderWithCorrectItemDetails() {
            addItemToCart(testCart.getId(), testItem1.getId(), 3).block();
            addItemToCart(testCart.getId(), testItem2.getId(), 2).block();
            addItemToCart(testCart.getId(), testItem3.getId(), 1).block();
            Order createdOrder = orderService.createOrderFromCart(testCart.getId()).block();

            StepVerifier.create(orderService.getOrderById(Objects.requireNonNull(createdOrder).getId()))
                    .assertNext(orderDto -> {
                        assertThat(orderDto.items()).hasSize(3);

                        OrderItemDto orangeItem = orderDto.items().stream()
                                .filter(i -> i.title().equals("Апельсин"))
                                .findFirst().orElse(null);
                        assertThat(orangeItem).isNotNull();
                        assertThat(orangeItem.count()).isEqualTo(3);
                        assertThat(orangeItem.price()).isEqualTo(100L);
                        assertThat(orangeItem.subtotal()).isEqualTo(300L);

                        OrderItemDto bananaItem = orderDto.items().stream()
                                .filter(i -> i.title().equals("Банан"))
                                .findFirst().orElse(null);
                        assertThat(bananaItem).isNotNull();
                        assertThat(bananaItem.count()).isEqualTo(2);
                        assertThat(bananaItem.price()).isEqualTo(200L);
                        assertThat(bananaItem.subtotal()).isEqualTo(400L);

                        OrderItemDto appleItem = orderDto.items().stream()
                                .filter(i -> i.title().equals("Яблоко"))
                                .findFirst().orElse(null);
                        assertThat(appleItem).isNotNull();
                        assertThat(appleItem.count()).isEqualTo(1);
                        assertThat(appleItem.price()).isEqualTo(150L);
                        assertThat(appleItem.subtotal()).isEqualTo(150L);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Тесты генерации номера заказа")
    class OrderNumberGenerationTests {

        @Test
        @DisplayName("Номер заказа должен иметь правильный формат")
        void shouldHaveCorrectOrderNumberFormat() {
            addItemToCart(testCart.getId(), testItem1.getId(), 1).block();
            Order order = orderService.createOrderFromCart(testCart.getId()).block();

            assertThat(Objects.requireNonNull(order).getOrderNumber()).matches("ORDER-\\d{14}-\\d{5}");
        }
    }
}