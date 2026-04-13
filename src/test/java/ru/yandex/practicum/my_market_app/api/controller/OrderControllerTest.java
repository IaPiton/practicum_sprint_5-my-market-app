package ru.yandex.practicum.my_market_app.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_app.api.handler.CartNotFoundException;
import ru.yandex.practicum.my_market_app.core.model.OrderDto;
import ru.yandex.practicum.my_market_app.core.model.OrderItemDto;
import ru.yandex.practicum.my_market_app.core.service.CartService;
import ru.yandex.practicum.my_market_app.core.service.OrderService;
import ru.yandex.practicum.my_market_app.persistence.entity.Order;
import ru.yandex.practicum.my_market_app.persistence.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@WebFluxTest(OrderController.class)
@DisplayName("Тесты контроллера заказов")
class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CartService cartService;

    @Test
    @DisplayName("POST /buy - должен создать заказ и перенаправить на страницу заказа")
    void buy_ShouldCreateOrderAndRedirect() {
        Long cartId = 1L;
        Order order = new Order();
        order.setId(100L);
        order.setOrderNumber("ORDER-20241201120000-12345");
        order.setTotalSum(1000L);

        when(cartService.getCurrentCartId(anyString())).thenReturn(Mono.just(cartId));
        when(orderService.createOrderFromCart(cartId)).thenReturn(Mono.just(order));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/100?newOrder=true");

        verify(orderService).createOrderFromCart(cartId);
    }

    @Test
    @DisplayName("POST /buy - при пустой корзине должен вернуть ошибку BAD_REQUEST")
    void buy_WhenCartIsEmpty_ShouldReturnBadRequest() {
        when(cartService.getCurrentCartId(anyString()))
                .thenReturn(Mono.error(new CartNotFoundException("Корзина пуста")));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("BAD_REQUEST")
                .jsonPath("$.message").isEqualTo("Корзина пуста");

        verify(orderService, never()).createOrderFromCart(anyLong());
    }

    @Test
    @DisplayName("POST /buy - при ошибке создания заказа должен вернуть INTERNAL_SERVER_ERROR")
    void buy_WhenOrderCreationFails_ShouldReturnInternalServerError() {
        Long cartId = 1L;

        when(cartService.getCurrentCartId(anyString())).thenReturn(Mono.just(cartId));
        when(orderService.createOrderFromCart(cartId)).thenReturn(Mono.error(new RuntimeException("Ошибка создания заказа")));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.error").isEqualTo("INTERNAL_SERVER")
                .jsonPath("$.message").isEqualTo("Произошла непредвиденная ошибка");

        verify(orderService).createOrderFromCart(cartId);
    }

    @Test
    @DisplayName("GET /orders/{id} - должен вернуть страницу заказа с параметром newOrder=true")
    void getOrder_WithNewOrderFlag_ShouldReturnOrderPage() {
        Long orderId = 100L;
        OrderDto orderDto = createTestOrderDto(orderId);

        when(orderService.getOrderById(eq(orderId))).thenReturn(Mono.just(orderDto));

        webTestClient.get()
                .uri("/orders/{id}?newOrder=true", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains(String.valueOf(orderDto.totalSum()));
                });

        verify(orderService).getOrderById(eq(orderId));
    }

    @Test
    @DisplayName("GET /orders/{id} - должен вернуть страницу заказа с параметром newOrder=false (по умолчанию)")
    void getOrder_WithoutNewOrderFlag_ShouldReturnOrderPageWithFalse() {
        Long orderId = 100L;
        OrderDto orderDto = createTestOrderDto(orderId);

        when(orderService.getOrderById(eq(orderId))).thenReturn(Mono.just(orderDto));

        webTestClient.get()
                .uri("/orders/{id}", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);

        verify(orderService).getOrderById(eq(orderId));
    }

    @Test
    @DisplayName("GET /orders - должен вернуть страницу со списком заказов")
    void getOrders_ShouldReturnOrdersPage() {
        List<OrderDto> orders = Arrays.asList(
                createTestOrderDto(1L),
                createTestOrderDto(2L),
                createTestOrderDto(3L)
        );

        when(orderService.getAllOrders()).thenReturn(Flux.fromIterable(orders));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Заказ №1");
                    assert body.contains("Заказ №2");
                    assert body.contains("Заказ №3");
                });

        verify(orderService).getAllOrders();
    }

     @Test
    @DisplayName("GET /orders - при ошибке сервиса должен вернуть INTERNAL_SERVER_ERROR")
    void getOrders_WhenServiceThrowsException_ShouldReturnInternalServerError() {
        when(orderService.getAllOrders())
                .thenReturn(Flux.error(new RuntimeException("Ошибка подключения к базе данных")));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.error").isEqualTo("INTERNAL_SERVER")
                .jsonPath("$.message").isEqualTo("Произошла непредвиденная ошибка");

        verify(orderService).getAllOrders();
    }

    @Test
    @DisplayName("GET /orders/{id} - при ошибке сервиса должен вернуть INTERNAL_SERVER_ERROR")
    void getOrder_WhenServiceThrowsUnexpectedException_ShouldReturnInternalServerError() {
        Long orderId = 100L;

        when(orderService.getOrderById(eq(orderId)))
                .thenReturn(Mono.error(new RuntimeException("Неожиданная ошибка")));

        webTestClient.get()
                .uri("/orders/{id}", orderId)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.error").isEqualTo("INTERNAL_SERVER")
                .jsonPath("$.message").isEqualTo("Произошла непредвиденная ошибка");

        verify(orderService).getOrderById(eq(orderId));
    }

    @Test
    @DisplayName("POST /buy - несколько запросов подряд должны создавать разные заказы")
    void buy_MultipleRequests_ShouldCreateDifferentOrders() {
        Long cartId1 = 1L;
        Long cartId2 = 2L;
        Order order1 = new Order();
        order1.setId(100L);
        order1.setOrderNumber("ORDER-100");
        Order order2 = new Order();
        order2.setId(200L);
        order2.setOrderNumber("ORDER-200");

        when(cartService.getCurrentCartId(anyString()))
                .thenReturn(Mono.just(cartId1))
                .thenReturn(Mono.just(cartId2));
        when(orderService.createOrderFromCart(cartId1)).thenReturn(Mono.just(order1));
        when(orderService.createOrderFromCart(cartId2)).thenReturn(Mono.just(order2));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/100?newOrder=true");

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/200?newOrder=true");

        verify(orderService).createOrderFromCart(cartId1);
        verify(orderService).createOrderFromCart(cartId2);
    }

    @Test
    @DisplayName("GET /orders/{id} - с деталями заказа должен содержать позиции")
    void getOrder_ShouldContainOrderItems() {
        Long orderId = 100L;
        OrderDto orderDto = createTestOrderDtoWithItems(orderId);

        when(orderService.getOrderById(eq(orderId))).thenReturn(Mono.just(orderDto));

        webTestClient.get()
                .uri("/orders/{id}", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Товар 1");
                    assert body.contains("Товар 2");
                    assert body.contains("200");
                    assert body.contains("150");
                });

        verify(orderService).getOrderById(eq(orderId));
    }

    @Test
    @DisplayName("GET /orders - страница должна содержать несколько заказов")
    void getOrders_ShouldDisplayMultipleOrders() {
        List<OrderDto> orders = Arrays.asList(
                createTestOrderDto(1L),
                createTestOrderDto(2L),
                createTestOrderDto(3L)
        );

        when(orderService.getAllOrders()).thenReturn(Flux.fromIterable(orders));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Заказ №1");
                    assert body.contains("Заказ №2");
                    assert body.contains("Заказ №3");
                    assert body.contains("1000");
                    assert body.contains("2000");
                    assert body.contains("3000");
                });

        verify(orderService).getAllOrders();
    }

    @Test
    @DisplayName("GET /orders/{id} - заказ должен содержать правильную общую сумму")
    void getOrder_ShouldHaveCorrectTotalSum() {
        Long orderId = 100L;
        OrderDto orderDto = createTestOrderDtoWithItems(orderId);
        long expectedTotal = 350L;

        when(orderService.getOrderById(eq(orderId))).thenReturn(Mono.just(orderDto));

        webTestClient.get()
                .uri("/orders/{id}", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains(String.valueOf(expectedTotal));
                });

        verify(orderService).getOrderById(eq(orderId));
    }

    private OrderDto createTestOrderDto(Long id) {
        return OrderDto.builder()
                .id(id)
                .orderNumber("ORDER-" + id)
                .totalSum(1000L * id)
                .status(OrderStatus.NEW.name())
                .createdAt(LocalDateTime.now())
                .items(Collections.emptyList())
                .build();
    }

    private OrderDto createTestOrderDtoWithItems(Long id) {
        List<OrderItemDto> items = Arrays.asList(
                OrderItemDto.builder()
                        .id(1L)
                        .title("Товар 1")
                        .price(100L)
                        .count(2)
                        .subtotal(200L)
                        .build(),
                OrderItemDto.builder()
                        .id(2L)
                        .title("Товар 2")
                        .price(150L)
                        .count(1)
                        .subtotal(150L)
                        .build()
        );

        return OrderDto.builder()
                .id(id)
                .orderNumber("ORDER-" + id)
                .totalSum(350L)
                .status(OrderStatus.NEW.name())
                .createdAt(LocalDateTime.now())
                .items(items)
                .build();
    }
}