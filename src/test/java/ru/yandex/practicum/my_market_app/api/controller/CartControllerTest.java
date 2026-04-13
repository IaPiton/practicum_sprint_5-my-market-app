package ru.yandex.practicum.my_market_app.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.my_market_app.api.handler.CartNotFoundException;
import ru.yandex.practicum.my_market_app.api.handler.ItemNotFoundException;
import ru.yandex.practicum.my_market_app.core.model.CartItemDto;
import ru.yandex.practicum.my_market_app.core.service.CartService;
import ru.yandex.practicum.my_market_app.core.service.ItemService;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CartController.class)
@DisplayName("Тесты контроллера корзины")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private ItemService itemService;


    @Test
    @DisplayName("GET /cart/items - должен вернуть страницу корзины с товарами")
    void getCartItems_ShouldReturnCartPageWithItems() throws Exception {
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

        MockHttpSession session = new MockHttpSession();
        String actualSessionId = session.getId();

        when(cartService.getCurrentCartId(actualSessionId)).thenReturn(cartId);
        when(cartService.getCartItemsWithDetails(cartId)).thenReturn(items);
        when(cartService.getCartTotal(cartId)).thenReturn(total);



        mockMvc.perform(get("/cart/items")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("items", items))
                .andExpect(model().attribute("total", total));

        verify(cartService).getCurrentCartId(actualSessionId);
        verify(cartService).getCartItemsWithDetails(cartId);
        verify(cartService).getCartTotal(cartId);
    }

    @Test
    @DisplayName("GET /cart/items - когда корзина пуста, должен вернуть пустую страницу корзины")
    void getCartItems_WhenCartIsEmpty_ShouldReturnEmptyCartPage() throws Exception {
        Long cartId = 1L;
        List<CartItemDto> emptyItems = Collections.emptyList();
        long total = 0L;

        MockHttpSession session = new MockHttpSession();
        String actualSessionId = session.getId();

        when(cartService.getCurrentCartId(actualSessionId)).thenReturn(cartId);
        when(cartService.getCartItemsWithDetails(cartId)).thenReturn(emptyItems);
        when(cartService.getCartTotal(cartId)).thenReturn(total);

        mockMvc.perform(get("/cart/items")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", emptyItems))
                .andExpect(model().attribute("total", 0L));

        verify(cartService).getCartItemsWithDetails(cartId);
        verify(cartService).getCartTotal(cartId);
    }

    @Test
    @DisplayName("POST /cart/items - действие PLUS должно увеличить количество товара в корзине")
    void updateCartItem_WithPlusAction_ShouldIncreaseItemCount() throws Exception {
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

        MockHttpSession session = new MockHttpSession();
        String actualSessionId = session.getId();

        when(cartService.getCurrentCartId(actualSessionId)).thenReturn(cartId);
        when(itemService.getItemEntityById(itemId)).thenReturn(item);
        doNothing().when(cartService).updateItemCount(eq(cartId), eq(item), eq(action));
        when(cartService.getCartItemsWithDetails(cartId)).thenReturn(updatedItems);
        when(cartService.getCartTotal(cartId)).thenReturn(total);



        mockMvc.perform(post("/cart/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", action)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("items", updatedItems))
                .andExpect(model().attribute("total", total));

        verify(cartService).getCurrentCartId(actualSessionId);
        verify(itemService).getItemEntityById(itemId);
        verify(cartService).updateItemCount(cartId, item, action);
        verify(cartService).getCartItemsWithDetails(cartId);
        verify(cartService).getCartTotal(cartId);
    }

    @Test
    @DisplayName("POST /cart/items - действие MINUS должно уменьшить количество товара в корзине")
    void updateCartItem_WithMinusAction_ShouldDecreaseItemCount() throws Exception {
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

        MockHttpSession session = new MockHttpSession();
        String actualSessionId = session.getId();

        when(cartService.getCurrentCartId(actualSessionId)).thenReturn(cartId);
        when(itemService.getItemEntityById(itemId)).thenReturn(item);
        doNothing().when(cartService).updateItemCount(eq(cartId), eq(item), eq(action));
        when(cartService.getCartItemsWithDetails(cartId)).thenReturn(updatedItems);
        when(cartService.getCartTotal(cartId)).thenReturn(total);

        mockMvc.perform(post("/cart/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", action)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", updatedItems))
                .andExpect(model().attribute("total", total));

        verify(cartService).updateItemCount(cartId, item, action);
    }

    @Test
    @DisplayName("POST /cart/items - действие DELETE должно удалить товар из корзины")
    void updateCartItem_WithDeleteAction_ShouldRemoveItemFromCart() throws Exception {
        Long cartId = 1L;
        Long itemId = 1L;
        String action = "DELETE";

        Item item = new Item();
        item.setId(itemId);
        item.setTitle("Тестовый товар");
        item.setPrice(100L);

        List<CartItemDto> updatedItems = Collections.emptyList();
        long total = 0L;

        MockHttpSession session = new MockHttpSession();
        String actualSessionId = session.getId();

        when(cartService.getCurrentCartId(actualSessionId)).thenReturn(cartId);
        when(itemService.getItemEntityById(itemId)).thenReturn(item);
        doNothing().when(cartService).updateItemCount(eq(cartId), eq(item), eq(action));
        when(cartService.getCartItemsWithDetails(cartId)).thenReturn(updatedItems);
        when(cartService.getCartTotal(cartId)).thenReturn(total);

        mockMvc.perform(post("/cart/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", action)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", updatedItems))
                .andExpect(model().attribute("total", 0L));

        verify(cartService).updateItemCount(cartId, item, action);
    }

    @Test
    @DisplayName("POST /cart/items - при ненайденной корзине должен вернуть ошибку BAD_REQUEST")
    void updateCartItem_WhenCartNotFound_ShouldReturnBadRequest() throws Exception {
        Long itemId = 1L;
        String action = "PLUS";

        MockHttpSession session = new MockHttpSession();
        String actualSessionId = session.getId();

        when(cartService.getCurrentCartId(actualSessionId)).thenThrow(new CartNotFoundException("Корзина не найдена"));

        mockMvc.perform(post("/cart/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", action)
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Корзина не найдена"));

        verify(cartService).getCurrentCartId(actualSessionId);
        verify(itemService, never()).getItemEntityById(anyLong());
    }

    @Test
    @DisplayName("POST /cart/items - при ненайденном товаре должен вернуть ошибку BAD_REQUEST")
    void updateCartItem_WhenItemNotFound_ShouldReturnBadRequest() throws Exception {
        Long cartId = 1L;
        Long invalidItemId = 999L;
        String action = "PLUS";

        MockHttpSession session = new MockHttpSession();
        String actualSessionId = session.getId();

        when(cartService.getCurrentCartId(actualSessionId)).thenReturn(cartId);
        when(itemService.getItemEntityById(invalidItemId))
                .thenThrow(new ItemNotFoundException("Товар с ID " + invalidItemId + " не найден"));

        mockMvc.perform(post("/cart/items")
                        .param("id", String.valueOf(invalidItemId))
                        .param("action", action)
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Товар с ID " + invalidItemId + " не найден"));

        verify(itemService).getItemEntityById(invalidItemId);
        verify(cartService, never()).updateItemCount(anyLong(), any(Item.class), anyString());
    }

    @Test
    @DisplayName("POST /cart/items - при неверном действии должен вернуть ошибку INTERNAL_SERVER_ERROR")
    void updateCartItem_WithInvalidAction_ShouldReturnInternalServerError() throws Exception {
        Long cartId = 1L;
        Long itemId = 1L;
        String invalidAction = "INVALID_ACTION";

        Item item = new Item();
        item.setId(itemId);

        MockHttpSession session = new MockHttpSession();
        String actualSessionId = session.getId();

        when(cartService.getCurrentCartId(actualSessionId)).thenReturn(cartId);
        when(itemService.getItemEntityById(itemId)).thenReturn(item);
        doThrow(new RuntimeException("Неизвестное действие: " + invalidAction))
                .when(cartService).updateItemCount(eq(cartId), eq(item), eq(invalidAction));

        mockMvc.perform(post("/cart/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", invalidAction)
                        .session(session))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER"))
                .andExpect(jsonPath("$.message").value("Произошла непредвиденная ошибка"));

        verify(cartService).updateItemCount(cartId, item, invalidAction);
    }

    @Test
    @DisplayName("POST /cart/items - без параметра action должен вернуть ошибку валидации")
    void updateCartItem_WithoutActionParam_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .param("id", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /cart/items - без параметра id должен вернуть ошибку валидации")
    void updateCartItem_WithoutIdParam_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .param("action", "PLUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /cart/items - при ошибке сервиса должен вернуть INTERNAL_SERVER_ERROR")
    void getCartItems_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        when(cartService.getCurrentCartId(anyString())).thenThrow(new RuntimeException("Ошибка подключения к базе данных"));

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER"))
                .andExpect(jsonPath("$.message").value("Произошла непредвиденная ошибка"));
    }

    @Test
    @DisplayName("GET /cart/items - должен корректно отображать несколько товаров в корзине")
    void getCartItems_WithMultipleItems_ShouldDisplayAllItems() throws Exception {
        Long cartId = 1L;
        List<CartItemDto> items = Arrays.asList(
                CartItemDto.builder().id(1L).title("Товар A").price(100L).count(2).subtotal(200L).build(),
                CartItemDto.builder().id(2L).title("Товар B").price(200L).count(1).subtotal(200L).build(),
                CartItemDto.builder().id(3L).title("Товар C").price(150L).count(3).subtotal(450L).build()
        );
        long total = 850L;

        MockHttpSession session = new MockHttpSession();
        String actualSessionId = session.getId();

        when(cartService.getCurrentCartId(actualSessionId)).thenReturn(cartId);
        when(cartService.getCartItemsWithDetails(cartId)).thenReturn(items);
        when(cartService.getCartTotal(cartId)).thenReturn(total);

        mockMvc.perform(get("/cart/items")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", items))
                .andExpect(model().attribute("total", total));

        verify(cartService).getCartItemsWithDetails(cartId);
        verify(cartService).getCartTotal(cartId);
    }
}