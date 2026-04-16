package ru.yandex.practicum.my_market_service.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.api.handler.CartNotFoundException;
import ru.yandex.practicum.my_market_service.api.handler.ItemNotFoundException;
import ru.yandex.practicum.my_market_service.core.model.CartItemDto;
import ru.yandex.practicum.my_market_service.core.service.CartService;
import ru.yandex.practicum.my_market_service.core.service.ItemService;
import ru.yandex.practicum.my_market_service.persistence.entity.Item;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@WebFluxTest(CartController.class)
@DisplayName("Тесты контроллера корзины (WebFlux)")
class CartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private ItemService itemService;

    @Test
    @DisplayName("GET /cart/items - должен вернуть страницу корзины с товарами")
    void getCartItems_ShouldReturnCartPageWithItems() {
        Long cartId = 1L;
        List<CartItemDto> items = Arrays.asList(
                CartItemDto.builder()
                        .id(1L)
                        .title("Тестовый товар 1")
                        .description("Описание товара 1")
                        .imgPath("/images/test1.jpg")
                        .price(100L)
                        .count(2)
                        .subtotal(200L)
                        .build(),
                CartItemDto.builder()
                        .id(2L)
                        .title("Тестовый товар 2")
                        .description("Описание товара 2")
                        .imgPath("/images/test2.jpg")
                        .price(150L)
                        .count(1)
                        .subtotal(150L)
                        .build()
        );
        long total = 350L;

        when(cartService.getCurrentCartId(anyString())).thenReturn(Mono.just(cartId));
        when(cartService.getCartItemsWithDetails(cartId)).thenReturn(Flux.fromIterable(items));
        when(cartService.getCartTotal(cartId)).thenReturn(Mono.just(total));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Тестовый товар 1");
                    assert body.contains("Тестовый товар 2");
                    assert body.contains("350");
                });

        verify(cartService).getCurrentCartId(anyString());
        verify(cartService).getCartItemsWithDetails(cartId);
        verify(cartService).getCartTotal(cartId);
    }

    @Test
    @DisplayName("GET /cart/items - когда корзина пуста, должен вернуть пустую страницу корзины")
    void getCartItems_WhenCartIsEmpty_ShouldReturnEmptyCartPage() {
        Long cartId = 1L;
        List<CartItemDto> emptyItems = Collections.emptyList();
        long total = 0L;

        when(cartService.getCurrentCartId(anyString())).thenReturn(Mono.just(cartId));
        when(cartService.getCartItemsWithDetails(cartId)).thenReturn(Flux.fromIterable(emptyItems));
        when(cartService.getCartTotal(cartId)).thenReturn(Mono.just(total));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Корзина пуста") || body.contains("0");
                });

        verify(cartService).getCartItemsWithDetails(cartId);
        verify(cartService).getCartTotal(cartId);
    }

    @Test
    @DisplayName("POST /cart/items - действие PLUS должно увеличить количество товара в корзине")
    void updateCartItem_WithPlusAction_ShouldIncreaseItemCount() {
        Long cartId = 1L;
        Long itemId = 1L;
        String action = "PLUS";

        Item item = new Item();
        item.setId(itemId);
        item.setTitle("Тестовый товар");
        item.setPrice(100L);

        List<CartItemDto> updatedItems = List.of(
                CartItemDto.builder()
                        .id(1L)
                        .title("Тестовый товар")
                        .price(100L)
                        .count(3)
                        .subtotal(300L)
                        .build()
        );
        long total = 300L;

        when(cartService.getCurrentCartId(anyString())).thenReturn(Mono.just(cartId));
        when(itemService.getItemEntityById(itemId)).thenReturn(Mono.just(item));
        when(cartService.updateItemCount(eq(cartId), eq(item), eq(action))).thenReturn(Mono.empty());
        when(cartService.getCartItemsWithDetails(cartId)).thenReturn(Flux.fromIterable(updatedItems));
        when(cartService.getCartTotal(cartId)).thenReturn(Mono.just(total));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=" + action)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("300");
                });

        verify(cartService).getCurrentCartId(anyString());
        verify(itemService).getItemEntityById(itemId);
        verify(cartService).updateItemCount(cartId, item, action);
        verify(cartService).getCartItemsWithDetails(cartId);
        verify(cartService).getCartTotal(cartId);
    }

    @Test
    @DisplayName("POST /cart/items - действие MINUS должно уменьшить количество товара в корзине")
    void updateCartItem_WithMinusAction_ShouldDecreaseItemCount() {
        Long cartId = 1L;
        Long itemId = 1L;
        String action = "MINUS";

        Item item = new Item();
        item.setId(itemId);
        item.setTitle("Тестовый товар");
        item.setPrice(100L);

        List<CartItemDto> updatedItems = List.of(
                CartItemDto.builder()
                        .id(1L)
                        .title("Тестовый товар")
                        .price(100L)
                        .count(1)
                        .subtotal(100L)
                        .build()
        );
        long total = 100L;

        when(cartService.getCurrentCartId(anyString())).thenReturn(Mono.just(cartId));
        when(itemService.getItemEntityById(itemId)).thenReturn(Mono.just(item));
        when(cartService.updateItemCount(eq(cartId), eq(item), eq(action))).thenReturn(Mono.empty());
        when(cartService.getCartItemsWithDetails(cartId)).thenReturn(Flux.fromIterable(updatedItems));
        when(cartService.getCartTotal(cartId)).thenReturn(Mono.just(total));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=" + action)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("100");
                });

        verify(cartService).updateItemCount(cartId, item, action);
    }

    @Test
    @DisplayName("POST /cart/items - действие DELETE должно удалить товар из корзины")
    void updateCartItem_WithDeleteAction_ShouldRemoveItemFromCart() {
        Long cartId = 1L;
        Long itemId = 1L;
        String action = "DELETE";

        Item item = new Item();
        item.setId(itemId);
        item.setTitle("Тестовый товар");
        item.setPrice(100L);

        List<CartItemDto> updatedItems = Collections.emptyList();
        long total = 0L;

        when(cartService.getCurrentCartId(anyString())).thenReturn(Mono.just(cartId));
        when(itemService.getItemEntityById(itemId)).thenReturn(Mono.just(item));
        when(cartService.updateItemCount(eq(cartId), eq(item), eq(action))).thenReturn(Mono.empty());
        when(cartService.getCartItemsWithDetails(cartId)).thenReturn(Flux.fromIterable(updatedItems));
        when(cartService.getCartTotal(cartId)).thenReturn(Mono.just(total));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=" + action)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("0") || body.contains("пуста");
                });

        verify(cartService).updateItemCount(cartId, item, action);
    }

    @Test
    @DisplayName("POST /cart/items - при ненайденной корзине должен вернуть ошибку BAD_REQUEST")
    void updateCartItem_WhenCartNotFound_ShouldReturnBadRequest() {
        long itemId = 1L;
        String action = "PLUS";

        when(cartService.getCurrentCartId(anyString()))
                .thenReturn(Mono.error(new CartNotFoundException("Корзина не найдена")));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=" + action)
                .exchange()
                .expectStatus().isBadRequest();

        verify(cartService).getCurrentCartId(anyString());
        verify(itemService, never()).getItemEntityById(anyLong());
    }

    @Test
    @DisplayName("POST /cart/items - при ненайденном товаре должен вернуть ошибку BAD_REQUEST")
    void updateCartItem_WhenItemNotFound_ShouldReturnBadRequest() {
        Long cartId = 1L;
        Long invalidItemId = 999L;
        String action = "PLUS";

        when(cartService.getCurrentCartId(anyString())).thenReturn(Mono.just(cartId));
        when(itemService.getItemEntityById(invalidItemId))
                .thenReturn(Mono.error(new ItemNotFoundException("Товар с ID " + invalidItemId + " не найден")));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + invalidItemId + "&action=" + action)
                .exchange()
                .expectStatus().isBadRequest();

        verify(itemService).getItemEntityById(invalidItemId);
        verify(cartService, never()).updateItemCount(anyLong(), any(Item.class), anyString());
    }

    @Test
    @DisplayName("POST /cart/items - при неверном действии должен вернуть ошибку INTERNAL_SERVER_ERROR")
    void updateCartItem_WithInvalidAction_ShouldReturnInternalServerError() {
        Long cartId = 1L;
        Long itemId = 1L;
        String invalidAction = "INVALID_ACTION";

        Item item = new Item();
        item.setId(itemId);

        when(cartService.getCurrentCartId(anyString())).thenReturn(Mono.just(cartId));
        when(itemService.getItemEntityById(itemId)).thenReturn(Mono.just(item));
        when(cartService.updateItemCount(eq(cartId), eq(item), eq(invalidAction)))
                .thenReturn(Mono.error(new RuntimeException("Неизвестное действие: " + invalidAction)));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=" + invalidAction)
                .exchange()
                .expectStatus().is5xxServerError();

        verify(cartService).updateItemCount(cartId, item, invalidAction);
    }

    @Test
    @DisplayName("GET /cart/items - при ошибке сервиса должен вернуть INTERNAL_SERVER_ERROR")
    void getCartItems_WhenServiceThrowsException_ShouldReturnInternalServerError() {
        when(cartService.getCurrentCartId(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Ошибка подключения к базе данных")));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("GET /cart/items - должен корректно отображать несколько товаров в корзине")
    void getCartItems_WithMultipleItems_ShouldDisplayAllItems() {
        Long cartId = 1L;
        List<CartItemDto> items = Arrays.asList(
                CartItemDto.builder().id(1L).title("Товар A").price(100L).count(2).subtotal(200L).build(),
                CartItemDto.builder().id(2L).title("Товар B").price(200L).count(1).subtotal(200L).build(),
                CartItemDto.builder().id(3L).title("Товар C").price(150L).count(3).subtotal(450L).build()
        );
        long total = 850L;

        when(cartService.getCurrentCartId(anyString())).thenReturn(Mono.just(cartId));
        when(cartService.getCartItemsWithDetails(cartId)).thenReturn(Flux.fromIterable(items));
        when(cartService.getCartTotal(cartId)).thenReturn(Mono.just(total));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Товар A");
                    assert body.contains("Товар B");
                    assert body.contains("Товар C");
                    assert body.contains("850");
                });

        verify(cartService).getCartItemsWithDetails(cartId);
        verify(cartService).getCartTotal(cartId);
    }

    @Test
    @DisplayName("POST /cart/items - несколько последовательных обновлений корзины")
    void updateCartItem_MultipleUpdates_ShouldHandleCorrectly() {
        Long cartId = 1L;
        Long itemId = 1L;

        Item item = new Item();
        item.setId(itemId);
        item.setTitle("Тестовый товар");
        item.setPrice(100L);

        List<CartItemDto> itemsAfterPlus = List.of(
                CartItemDto.builder().id(1L).title("Тестовый товар").price(100L).count(3).subtotal(300L).build()
        );
        long totalAfterPlus = 300L;

        when(cartService.getCurrentCartId(anyString())).thenReturn(Mono.just(cartId));
        when(itemService.getItemEntityById(itemId)).thenReturn(Mono.just(item));
        when(cartService.updateItemCount(eq(cartId), eq(item), eq("PLUS"))).thenReturn(Mono.empty());
        when(cartService.getCartItemsWithDetails(cartId)).thenReturn(Flux.fromIterable(itemsAfterPlus));
        when(cartService.getCartTotal(cartId)).thenReturn(Mono.just(totalAfterPlus));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=PLUS")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("300");
                });

        List<CartItemDto> itemsAfterMinus = List.of(
                CartItemDto.builder().id(1L).title("Тестовый товар").price(100L).count(2).subtotal(200L).build()
        );
        long totalAfterMinus = 200L;

        when(cartService.updateItemCount(eq(cartId), eq(item), eq("MINUS"))).thenReturn(Mono.empty());
        when(cartService.getCartItemsWithDetails(cartId)).thenReturn(Flux.fromIterable(itemsAfterMinus));
        when(cartService.getCartTotal(cartId)).thenReturn(Mono.just(totalAfterMinus));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=MINUS")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("200");
                });

        verify(cartService, times(2)).getCurrentCartId(anyString());
        verify(itemService, times(2)).getItemEntityById(itemId);
        verify(cartService).updateItemCount(cartId, item, "PLUS");
        verify(cartService).updateItemCount(cartId, item, "MINUS");
    }

    @Test
    @DisplayName("GET /cart/items - должен создавать новую сессию для разных запросов")
    void getCartItems_ShouldHandleSessionCorrectly() {
        Long cartId1 = 1L;
        Long cartId2 = 2L;

        List<CartItemDto> items1 = List.of(
                CartItemDto.builder().id(1L).title("Товар 1").price(100L).count(1).subtotal(100L).build()
        );
        long total1 = 100L;

        List<CartItemDto> items2 = List.of(
                CartItemDto.builder().id(2L).title("Товар 2").price(200L).count(1).subtotal(200L).build()
        );
        long total2 = 200L;

        when(cartService.getCurrentCartId(anyString()))
                .thenReturn(Mono.just(cartId1))
                .thenReturn(Mono.just(cartId2));
        when(cartService.getCartItemsWithDetails(cartId1)).thenReturn(Flux.fromIterable(items1));
        when(cartService.getCartTotal(cartId1)).thenReturn(Mono.just(total1));
        when(cartService.getCartItemsWithDetails(cartId2)).thenReturn(Flux.fromIterable(items2));
        when(cartService.getCartTotal(cartId2)).thenReturn(Mono.just(total2));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Товар 1");
                    assert body.contains("100");
                });

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Товар 2");
                    assert body.contains("200");
                });

        verify(cartService, times(2)).getCurrentCartId(anyString());
    }
}