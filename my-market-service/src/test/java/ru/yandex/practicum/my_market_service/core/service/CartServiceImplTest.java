package ru.yandex.practicum.my_market_service.core.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.my_market_service.configuration.TestcontainersTest;
import ru.yandex.practicum.my_market_service.core.model.CartItemDto;
import ru.yandex.practicum.my_market_service.core.security.OAuth2Service;
import ru.yandex.practicum.my_market_service.core.security.SecurityService;
import ru.yandex.practicum.my_market_service.persistence.entity.Cart;
import ru.yandex.practicum.my_market_service.persistence.entity.Item;
import ru.yandex.practicum.my_market_service.persistence.repository.CartItemRepository;
import ru.yandex.practicum.my_market_service.persistence.repository.CartRepository;
import ru.yandex.practicum.my_market_service.persistence.repository.ItemRepository;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Интеграционные тесты сервиса корзины с реальной БД")
class CartServiceImplTest extends TestcontainersTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private OAuth2Service oAuth2Service;

    @Autowired
    private ItemRepository itemRepository;

    private Cart testCart;
    private Item testItem1;
    private Item testItem2;
    private Item testItem3;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll().block();
        cartRepository.deleteAll().block();
        itemRepository.deleteAll().block();

        testCart = new Cart();
        Long userId = 3L;
        testCart.setUserId(userId);
        testCart = cartRepository.save(testCart).block();

        testItem1 = new Item();
        testItem1.setTitle("Товар A");
        testItem1.setDescription("Описание товара A");
        testItem1.setImgPath("/images/test1.jpg");
        testItem1.setPrice(100L);
        testItem1 = itemRepository.save(testItem1).block();

        testItem2 = new Item();
        testItem2.setTitle("Товар B");
        testItem2.setDescription("Описание товара B");
        testItem2.setImgPath("/images/test2.jpg");
        testItem2.setPrice(200L);
        testItem2 = itemRepository.save(testItem2).block();

        testItem3 = new Item();
        testItem3.setTitle("Товар C");
        testItem3.setDescription("Описание товара C");
        testItem3.setImgPath("/images/test3.jpg");
        testItem3.setPrice(150L);
        testItem3 = itemRepository.save(testItem3).block();
    }

    @AfterEach
    void tearDown() {
        cartItemRepository.deleteAll().block();
        cartRepository.deleteAll().block();
        itemRepository.deleteAll().block();
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
    @DisplayName("Тесты получения ID текущей корзины")
    class GetCurrentCartIdTests {

        @Test
        @DisplayName("Должен создать новую корзину, если её не существует")
        void shouldCreateNewCartWhenNotExists() {

            StepVerifier.create(
                    cartService.getCurrentCartId()
                            .flatMap(cartId ->
                                    cartRepository.findById(cartId)
                                            .map(savedCart -> new Object() {
                                                final Long id = cartId;
                                                final Cart cart = savedCart;
                                            })
                            )
            ).assertNext(pair -> {
                assertThat(pair.id).isNotNull();
            }).verifyComplete();
        }

        @Test
        @DisplayName("Должен вернуть существующую корзину по sessionId")
        void shouldReturnExistingCartBySessionId() {
            StepVerifier.create(cartService.getCurrentCartId())
                    .assertNext(cartId -> assertThat(cartId).isEqualTo(testCart.getId()))
                    .verifyComplete();
        }

        @Nested
        @DisplayName("Тесты получения количества товаров в корзине")
        class GetItemCountsTests {

            @Test
            @DisplayName("Должен вернуть пустую карту для пустой корзины")
            void shouldReturnEmptyMapForEmptyCart() {
                StepVerifier.create(cartService.getItemCounts(testCart.getId()))
                        .assertNext(counts -> {
                            assertThat(counts).isNotNull();
                            assertThat(counts).isEmpty();
                        })
                        .verifyComplete();
            }

            @Test
            @DisplayName("Должен вернуть правильное количество товаров в корзине")
            void shouldReturnCorrectItemCounts() {
                addItemToCart(testCart.getId(), testItem1.getId(), 2).block();
                addItemToCart(testCart.getId(), testItem2.getId(), 3).block();

                StepVerifier.create(cartService.getItemCounts(testCart.getId()))
                        .assertNext(counts -> {
                            assertThat(counts).hasSize(2);
                            assertThat(counts.get(testItem1.getId())).isEqualTo(2);
                            assertThat(counts.get(testItem2.getId())).isEqualTo(3);
                        })
                        .verifyComplete();
            }

            @Test
            @DisplayName("Должен вернуть пустую карту для несуществующей корзины")
            void shouldReturnEmptyMapForNonExistentCart() {
                StepVerifier.create(cartService.getItemCounts(99999L))
                        .assertNext(counts -> {
                            assertThat(counts).isNotNull();
                            assertThat(counts).isEmpty();
                        })
                        .verifyComplete();
            }
        }

        @Nested
        @DisplayName("Тесты обновления количества товара в корзине")
        class UpdateItemCountTests {

            @Test
            @DisplayName("Должен добавить новый товар в корзину при действии PLUS")
            void shouldAddNewItemToCartWhenPlusAction() {
                StepVerifier.create(cartService.updateItemCount(testCart.getId(), testItem1, "PLUS")
                                .thenMany(cartItemRepository.findByCartId(testCart.getId())))
                        .assertNext(cartItem -> {
                            assertThat(cartItem.getItemId()).isEqualTo(testItem1.getId());
                            assertThat(cartItem.getQuantity()).isEqualTo(1);
                        })
                        .verifyComplete();
            }

            @Test
            @DisplayName("Должен увеличить количество существующего товара при действии PLUS")
            void shouldIncreaseExistingItemQuantityWhenPlusAction() {
                addItemToCart(testCart.getId(), testItem1.getId(), 2).block();

                StepVerifier.create(cartService.updateItemCount(testCart.getId(), testItem1, "PLUS")
                                .then(cartItemRepository.findByCartIdAndItemId(testCart.getId(), testItem1.getId())))
                        .assertNext(cartItem -> {
                            assertThat(cartItem).isNotNull();
                            assertThat(cartItem.getQuantity()).isEqualTo(3);
                        })
                        .verifyComplete();
            }

            @Test
            @DisplayName("Должен уменьшить количество товара при действии MINUS")
            void shouldDecreaseItemQuantityWhenMinusAction() {
                addItemToCart(testCart.getId(), testItem1.getId(), 3).block();

                StepVerifier.create(cartService.updateItemCount(testCart.getId(), testItem1, "MINUS")
                                .then(cartItemRepository.findByCartIdAndItemId(testCart.getId(), testItem1.getId())))
                        .assertNext(cartItem -> {
                            assertThat(cartItem).isNotNull();
                            assertThat(cartItem.getQuantity()).isEqualTo(2);
                        })
                        .verifyComplete();
            }

            @Test
            @DisplayName("Должен удалить товар из корзины при уменьшении до 0")
            void shouldRemoveItemWhenQuantityBecomesZero() {
                addItemToCart(testCart.getId(), testItem1.getId(), 1).block();

                StepVerifier.create(cartService.updateItemCount(testCart.getId(), testItem1, "MINUS")
                                .then(cartItemRepository.findByCartIdAndItemId(testCart.getId(), testItem1.getId())))
                        .verifyComplete();
            }

            @Test
            @DisplayName("Должен удалить товар из корзины при действии DELETE")
            void shouldRemoveItemWhenDeleteAction() {
                addItemToCart(testCart.getId(), testItem1.getId(), 5).block();

                StepVerifier.create(cartService.updateItemCount(testCart.getId(), testItem1, "DELETE")
                                .then(cartItemRepository.findByCartIdAndItemId(testCart.getId(), testItem1.getId())))
                        .verifyComplete();
            }

            @Test
            @DisplayName("Должен ничего не делать при MINUS для отсутствующего товара")
            void shouldDoNothingWhenMinusOnNonExistentItem() {
                StepVerifier.create(cartService.updateItemCount(testCart.getId(), testItem1, "MINUS")
                                .thenMany(cartItemRepository.findByCartId(testCart.getId())))
                        .expectNextCount(0)
                        .verifyComplete();
            }

            @Test
            @DisplayName("Должен выбросить исключение при неверном действии")
            void shouldThrowExceptionWhenInvalidAction() {
                StepVerifier.create(cartService.updateItemCount(testCart.getId(), testItem1, "INVALID"))
                        .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().contains("Неизвестное действие"))
                        .verify();
            }

        }

        @Nested
        @DisplayName("Тесты получения товаров в корзине с деталями")
        class GetCartItemsWithDetailsTests {

            @Test
            @DisplayName("Должен вернуть пустой список для пустой корзины")
            void shouldReturnEmptyListForEmptyCart() {
                StepVerifier.create(cartService.getCartItemsWithDetails(testCart.getId()).collectList())
                        .assertNext(items -> {
                            assertThat(items).isNotNull();
                            assertThat(items).isEmpty();
                        })
                        .verifyComplete();
            }

            @Test
            @DisplayName("Должен вернуть список товаров с правильными деталями")
            void shouldReturnItemsWithCorrectDetails() {
                addItemToCart(testCart.getId(), testItem1.getId(), 2).block();
                addItemToCart(testCart.getId(), testItem2.getId(), 1).block();

                StepVerifier.create(cartService.getCartItemsWithDetails(testCart.getId()).collectList())
                        .assertNext(items -> {
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
                        })
                        .verifyComplete();
            }

            @Test
            @DisplayName("Должен вернуть пустой список для несуществующей корзины")
            void shouldReturnEmptyListForNonExistentCart() {
                StepVerifier.create(cartService.getCartItemsWithDetails(99999L).collectList())
                        .assertNext(items -> {
                            assertThat(items).isNotNull();
                            assertThat(items).isEmpty();
                        })
                        .verifyComplete();
            }
        }

        @Nested
        @DisplayName("Тесты получения общей суммы корзины")
        class GetCartTotalTests {

            @Test
            @DisplayName("Должен вернуть 0 для пустой корзины")
            void shouldReturnZeroForEmptyCart() {
                StepVerifier.create(cartService.getCartTotal(testCart.getId()))
                        .assertNext(total -> assertThat(total).isZero())
                        .verifyComplete();
            }

            @Test
            @DisplayName("Должен вернуть правильную сумму для корзины с товарами")
            void shouldReturnCorrectTotalForCartWithItems() {
                addItemToCart(testCart.getId(), testItem1.getId(), 2).block();
                addItemToCart(testCart.getId(), testItem2.getId(), 3).block();
                addItemToCart(testCart.getId(), testItem3.getId(), 1).block();

                StepVerifier.create(cartService.getCartTotal(testCart.getId()))
                        .assertNext(total -> assertThat(total).isEqualTo(200 + 600 + 150))
                        .verifyComplete();
            }

            @Test
            @DisplayName("Должен вернуть 0 для несуществующей корзины")
            void shouldReturnZeroForNonExistentCart() {
                StepVerifier.create(cartService.getCartTotal(99999L))
                        .assertNext(total -> assertThat(total).isZero())
                        .verifyComplete();
            }

            @Test
            @DisplayName("Сумма должна обновляться после изменений в корзине")
            void shouldUpdateTotalAfterCartChanges() {
                addItemToCart(testCart.getId(), testItem1.getId(), 2).block();

                StepVerifier.create(cartService.getCartTotal(testCart.getId()))
                        .assertNext(initialTotal -> assertThat(initialTotal).isEqualTo(200))
                        .verifyComplete();

                StepVerifier.create(cartService.updateItemCount(testCart.getId(), testItem1, "PLUS")
                                .then(cartService.getCartTotal(testCart.getId())))
                        .assertNext(afterPlusTotal -> assertThat(afterPlusTotal).isEqualTo(300))
                        .verifyComplete();

                StepVerifier.create(cartService.updateItemCount(testCart.getId(), testItem1, "DELETE")
                                .then(cartService.getCartTotal(testCart.getId())))
                        .assertNext(afterDeleteTotal -> assertThat(afterDeleteTotal).isZero())
                        .verifyComplete();
            }
        }

        @Nested
        @DisplayName("Интеграционные тесты потока данных")
        class IntegrationFlowTests {

            @Test
            @DisplayName("Должен корректно обработать полный цикл операций с корзиной")
            void shouldHandleFullCartCycle() {
                StepVerifier.create(
                        cartService.updateItemCount(testCart.getId(), testItem1, "PLUS")
                                .then(cartService.updateItemCount(testCart.getId(), testItem1, "PLUS"))
                                .then(cartService.updateItemCount(testCart.getId(), testItem2, "PLUS"))
                                .then(cartService.getItemCounts(testCart.getId()))
                ).assertNext(counts -> {
                    assertThat(counts.get(testItem1.getId())).isEqualTo(2);
                    assertThat(counts.get(testItem2.getId())).isEqualTo(1);
                }).verifyComplete();

                StepVerifier.create(cartService.getCartTotal(testCart.getId()))
                        .assertNext(total -> assertThat(total).isEqualTo(100 * 2 + 200))
                        .verifyComplete();

                StepVerifier.create(
                        cartService.updateItemCount(testCart.getId(), testItem1, "MINUS")
                                .then(cartService.getItemCounts(testCart.getId()))
                ).assertNext(counts -> assertThat(counts.get(testItem1.getId())).isEqualTo(1)).verifyComplete();

                StepVerifier.create(cartService.getCartTotal(testCart.getId()))
                        .assertNext(total -> assertThat(total).isEqualTo(100 + 200))
                        .verifyComplete();

                StepVerifier.create(
                        cartService.updateItemCount(testCart.getId(), testItem2, "DELETE")
                                .then(cartService.getItemCounts(testCart.getId()))
                ).assertNext(counts -> assertThat(counts).doesNotContainKey(testItem2.getId())).verifyComplete();

                StepVerifier.create(cartService.getCartTotal(testCart.getId()))
                        .assertNext(total -> assertThat(total).isEqualTo(100))
                        .verifyComplete();
            }

            @Test
            @DisplayName("Должен корректно обработать множество операций с разными товарами")
            void shouldHandleMultipleOperationsWithDifferentItems() {
                StepVerifier.create(
                        Mono.when(
                                Flux.range(1, 5).concatMap(i -> cartService.updateItemCount(testCart.getId(), testItem1, "PLUS")).then(),
                                Flux.range(1, 3).concatMap(i -> cartService.updateItemCount(testCart.getId(), testItem2, "PLUS")).then(),
                                Flux.range(1, 2).concatMap(i -> cartService.updateItemCount(testCart.getId(), testItem3, "PLUS")).then()
                        ).then(cartService.getItemCounts(testCart.getId()))
                ).assertNext(counts -> {
                    assertThat(counts.get(testItem1.getId())).isEqualTo(5);
                    assertThat(counts.get(testItem2.getId())).isEqualTo(3);
                    assertThat(counts.get(testItem3.getId())).isEqualTo(2);
                }).verifyComplete();

                StepVerifier.create(cartService.getCartTotal(testCart.getId()))
                        .assertNext(total -> assertThat(total).isEqualTo(100 * 5 + 200 * 3 + 150 * 2))
                        .verifyComplete();

                StepVerifier.create(
                        cartService.updateItemCount(testCart.getId(), testItem1, "DELETE")
                                .then(cartService.updateItemCount(testCart.getId(), testItem2, "DELETE"))
                                .then(cartService.updateItemCount(testCart.getId(), testItem3, "DELETE"))
                                .then(cartService.getCartItemsWithDetails(testCart.getId()).collectList())
                ).assertNext(items -> assertThat(items).isEmpty()).verifyComplete();

                StepVerifier.create(cartService.getCartTotal(testCart.getId()))
                        .assertNext(total -> assertThat(total).isZero())
                        .verifyComplete();
            }

            @Test
            @DisplayName("Должен корректно обработать DELETE для отсутствующего товара")
            void shouldHandleDeleteForNonExistentItem() {
                StepVerifier.create(
                                cartService.updateItemCount(testCart.getId(), testItem1, "DELETE")
                                        .thenMany(cartItemRepository.findByCartId(testCart.getId()))
                        ).expectNextCount(0)
                        .verifyComplete();
            }
        }
    }
}