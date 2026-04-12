package ru.yandex.practicum.my_market_app.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.yandex.practicum.my_market_app.api.handler.CartNotFoundException;
import ru.yandex.practicum.my_market_app.core.model.OrderDto;
import ru.yandex.practicum.my_market_app.core.model.OrderItemDto;
import ru.yandex.practicum.my_market_app.core.service.CartService;
import ru.yandex.practicum.my_market_app.core.service.OrderService;
import ru.yandex.practicum.my_market_app.persistence.entity.Order;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
@DisplayName("Тесты контроллера заказов")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CartService cartService;

    @Test
    @DisplayName("POST /buy - должен создать заказ и перенаправить на страницу заказа")
    void buy_ShouldCreateOrderAndRedirect() throws Exception {
        Long cartId = 1L;
        Order order = new Order();
        order.setId(100L);
        order.setOrderNumber("ORDER-20241201120000-12345");
        order.setTotalSum(1000L);

        when(cartService.getCurrentCartId(anyString())).thenReturn(cartId);
        when(orderService.createOrderFromCart(cartId)).thenReturn(order);

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/100?newOrder=true"));

        verify(orderService).createOrderFromCart(cartId);
    }

    @Test
    @DisplayName("POST /buy - при пустой корзине должен вернуть ошибку BAD_REQUEST")
    void buy_WhenCartIsEmpty_ShouldReturnBadRequest() throws Exception {
        when(cartService.getCurrentCartId(anyString()))
                .thenThrow(new CartNotFoundException("Корзина пуста"));

        mockMvc.perform(post("/buy"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Корзина пуста"));

        verify(orderService, never()).createOrderFromCart(anyLong());
    }

    @Test
    @DisplayName("POST /buy - при ошибке создания заказа должен вернуть INTERNAL_SERVER_ERROR")
    void buy_WhenOrderCreationFails_ShouldReturnInternalServerError() throws Exception {
        Long cartId = 1L;

        when(cartService.getCurrentCartId(anyString())).thenReturn(cartId);
        when(orderService.createOrderFromCart(cartId)).thenThrow(new RuntimeException("Ошибка создания заказа"));

        mockMvc.perform(post("/buy"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER"))
                .andExpect(jsonPath("$.message").value("Произошла непредвиденная ошибка"));


        verify(orderService).createOrderFromCart(cartId);
    }

    @Test
    @DisplayName("GET /orders/{id} - должен вернуть страницу заказа с параметром newOrder=true")
    void getOrder_WithNewOrderFlag_ShouldReturnOrderPage() throws Exception {
        Long orderId = 100L;
        OrderDto orderDto = createTestOrderDto(orderId);

        when(orderService.getOrderById(eq(orderId))).thenReturn(orderDto);

        mockMvc.perform(get("/orders/{id}", orderId)
                        .param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attributeExists("newOrder"))
                .andExpect(model().attribute("order", orderDto))
                .andExpect(model().attribute("newOrder", true));

        verify(orderService).getOrderById(eq(orderId));
    }

    @Test
    @DisplayName("GET /orders/{id} - должен вернуть страницу заказа с параметром newOrder=false (по умолчанию)")
    void getOrder_WithoutNewOrderFlag_ShouldReturnOrderPageWithFalse() throws Exception {
        Long orderId = 100L;
        OrderDto orderDto = createTestOrderDto(orderId);

        when(orderService.getOrderById(eq(orderId))).thenReturn(orderDto);

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("order", orderDto))
                .andExpect(model().attribute("newOrder", false));

        verify(orderService).getOrderById(eq(orderId));
    }


    @Test
    @DisplayName("GET /orders - должен вернуть страницу со списком заказов")
    void getOrders_ShouldReturnOrdersPage() throws Exception {
        List<OrderDto> orders = Arrays.asList(
                createTestOrderDto(1L),
                createTestOrderDto(2L),
                createTestOrderDto(3L)
        );

        when(orderService.getAllOrders()).thenReturn(orders);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attribute("orders", orders));

        verify(orderService).getAllOrders();
    }

    @Test
    @DisplayName("GET /orders - когда у пользователя нет заказов, должен вернуть пустой список")
    void getOrders_WhenNoOrders_ShouldReturnEmptyList() throws Exception {
        List<OrderDto> emptyOrders = Collections.emptyList();

        when(orderService.getAllOrders()).thenReturn(emptyOrders);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attribute("orders", emptyOrders));

        verify(orderService).getAllOrders();
    }

    @Test
    @DisplayName("GET /orders - при ошибке сервиса должен вернуть INTERNAL_SERVER_ERROR")
    void getOrders_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        when(orderService.getAllOrders()).thenThrow(new RuntimeException("Ошибка подключения к базе данных"));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER"))
                .andExpect(jsonPath("$.message").value("Произошла непредвиденная ошибка"));

        verify(orderService).getAllOrders();
    }

    @Test
    @DisplayName("GET /orders/{id} - при ошибке сервиса должен вернуть INTERNAL_SERVER_ERROR")
    void getOrder_WhenServiceThrowsUnexpectedException_ShouldReturnInternalServerError() throws Exception {
        Long orderId = 100L;

        when(orderService.getOrderById(eq(orderId)))
                .thenThrow(new RuntimeException("Неожиданная ошибка"));

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER"))
                .andExpect(jsonPath("$.message").value("Произошла непредвиденная ошибка"));

        verify(orderService).getOrderById(eq(orderId));
    }

    @Test
    @DisplayName("POST /buy - несколько запросов подряд должны создавать разные заказы")
    void buy_MultipleRequests_ShouldCreateDifferentOrders() throws Exception {
        Long cartId1 = 1L;
        Long cartId2 = 2L;
        Order order1 = new Order();
        order1.setId(100L);
        Order order2 = new Order();
        order2.setId(200L);

        when(cartService.getCurrentCartId(anyString()))
                .thenReturn(cartId1)
                .thenReturn(cartId2);
        when(orderService.createOrderFromCart(cartId1)).thenReturn(order1);
        when(orderService.createOrderFromCart(cartId2)).thenReturn(order2);

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/100?newOrder=true"));

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/200?newOrder=true"));

        verify(orderService).createOrderFromCart(cartId1);
        verify(orderService).createOrderFromCart(cartId2);
    }

    @Test
    @DisplayName("GET /orders/{id} - с деталями заказа должен содержать позиции")
    void getOrder_ShouldContainOrderItems() throws Exception {
        Long orderId = 100L;
        OrderDto orderDto = createTestOrderDtoWithItems(orderId);

        when(orderService.getOrderById(eq(orderId))).thenReturn(orderDto);

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("order", orderDto));

        verify(orderService).getOrderById(eq(orderId));
    }

    @Test
    @DisplayName("GET /orders - страница должна содержать несколько заказов")
    void getOrders_ShouldDisplayMultipleOrders() throws Exception {
        List<OrderDto> orders = Arrays.asList(
                createTestOrderDto(1L),
                createTestOrderDto(2L),
                createTestOrderDto(3L)
        );

        when(orderService.getAllOrders()).thenReturn(orders);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attribute("orders", orders))
                .andExpect(model().attribute("orders", org.hamcrest.Matchers.hasSize(3)));

        verify(orderService).getAllOrders();
    }

    @Test
    @DisplayName("GET /orders/{id} - заказ должен содержать правильную общую сумму")
    void getOrder_ShouldHaveCorrectTotalSum() throws Exception {
        Long orderId = 100L;
        OrderDto orderDto = createTestOrderDtoWithItems(orderId);
        long expectedTotal = 350L;

        when(orderService.getOrderById(eq(orderId))).thenReturn(orderDto);

        MvcResult result = mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(model().attribute("order", orderDto))
                .andReturn();

        OrderDto actualOrder = (OrderDto) Objects.requireNonNull(result.getModelAndView()).getModel().get("order");
        assertThat(actualOrder.totalSum()).isEqualTo(expectedTotal);
        verify(orderService).getOrderById(eq(orderId));
    }


    private OrderDto createTestOrderDto(Long id) {
        return OrderDto.builder()
                .id(id)
                .orderNumber("ORDER-" + id)
                .totalSum(1000L * id)
                .status("NEW")
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
                .status("NEW")
                .createdAt(LocalDateTime.now())
                .items(items)
                .build();
    }
}