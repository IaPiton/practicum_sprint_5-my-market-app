package api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.my_market_app.Application;
import ru.yandex.practicum.my_market_app.api.controller.ItemController;
import ru.yandex.practicum.my_market_app.api.handler.ApiExceptionHandler;
import ru.yandex.practicum.my_market_app.api.handler.ItemNotFoundException;
import ru.yandex.practicum.my_market_app.core.model.ItemsPageData;
import ru.yandex.practicum.my_market_app.core.model.PagingInfo;
import ru.yandex.practicum.my_market_app.core.service.ItemService;
import ru.yandex.practicum.my_market_app.persistence.model.ItemDto;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ItemController.class)
@ContextConfiguration(classes = {Application.class, ApiExceptionHandler.class})
@DisplayName("Тесты контроллера товаров")
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @Test
    @DisplayName("GET /items - должен вернуть страницу витрины с товарами")
    void getItems_ShouldReturnItemsPageWithItems() throws Exception {
        List<List<ItemDto>> itemsGrid = new ArrayList<>();
        List<ItemDto> row = new ArrayList<>();
        row.add(ItemDto.builder()
                .id(1L)
                .title("Тестовый товар 1")
                .description("Описание товара 1")
                .imgPath("/images/test1.jpg")
                .price(100L)
                .count(0)
                .build());
        itemsGrid.add(row);

        PagingInfo pagingInfo = new PagingInfo(1, 5, false, false, 1, 1);
        ItemsPageData pageData = new ItemsPageData(itemsGrid, "", "NO", pagingInfo);

        when(itemService.getItemsPage(anyString(), anyString(), anyInt(), anyInt())).thenReturn(pageData);

        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("search"))
                .andExpect(model().attributeExists("sort"))
                .andExpect(model().attributeExists("paging"));

        verify(itemService).getItemsPage(eq(""), eq("NO"), eq(1), eq(5));
    }

    @Test
    @DisplayName("GET / - корневой путь должен вернуть страницу витрины с товарами")
    void getItems_FromRootPath_ShouldReturnItemsPage() throws Exception {
        List<List<ItemDto>> itemsGrid = new ArrayList<>();
        PagingInfo pagingInfo = new PagingInfo(1, 5, false, false, 0, 0);
        ItemsPageData pageData = new ItemsPageData(itemsGrid, "", "NO", pagingInfo);

        when(itemService.getItemsPage(anyString(), anyString(), anyInt(), anyInt())).thenReturn(pageData);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"));

        verify(itemService).getItemsPage(eq(""), eq("NO"), eq(1), eq(5));
    }

    @Test
    @DisplayName("GET /items - с параметром поиска должен вернуть отфильтрованные товары")
    void getItems_WithSearchParam_ShouldReturnFilteredItems() throws Exception {
        String searchQuery = "тест";
        List<List<ItemDto>> itemsGrid = new ArrayList<>();
        PagingInfo pagingInfo = new PagingInfo(1, 5, false, false, 1, 1);
        ItemsPageData pageData = new ItemsPageData(itemsGrid, searchQuery, "NO", pagingInfo);

        when(itemService.getItemsPage(eq(searchQuery), eq("NO"), eq(1), eq(5))).thenReturn(pageData);

        mockMvc.perform(get("/items")
                        .param("search", searchQuery))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attribute("search", searchQuery));

        verify(itemService).getItemsPage(eq(searchQuery), eq("NO"), eq(1), eq(5));
    }

    @Test
    @DisplayName("GET /items - с сортировкой ALPHA должен вернуть отсортированные по названию товары")
    void getItems_WithAlphaSort_ShouldReturnSortedItems() throws Exception {
        String sort = "ALPHA";
        List<List<ItemDto>> itemsGrid = new ArrayList<>();
        PagingInfo pagingInfo = new PagingInfo(1, 5, false, false, 1, 1);
        ItemsPageData pageData = new ItemsPageData(itemsGrid, "", sort, pagingInfo);

        when(itemService.getItemsPage(eq(""), eq(sort), eq(1), eq(5))).thenReturn(pageData);

        mockMvc.perform(get("/items")
                        .param("sort", sort))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attribute("sort", sort));

        verify(itemService).getItemsPage(eq(""), eq(sort), eq(1), eq(5));
    }

    @Test
    @DisplayName("GET /items - с сортировкой PRICE должен вернуть отсортированные по цене товары")
    void getItems_WithPriceSort_ShouldReturnItemsSortedByPrice() throws Exception {
        String sort = "PRICE";
        List<List<ItemDto>> itemsGrid = new ArrayList<>();
        PagingInfo pagingInfo = new PagingInfo(1, 5, false, false, 1, 1);
        ItemsPageData pageData = new ItemsPageData(itemsGrid, "", sort, pagingInfo);

        when(itemService.getItemsPage(eq(""), eq(sort), eq(1), eq(5))).thenReturn(pageData);

        mockMvc.perform(get("/items")
                        .param("sort", sort))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attribute("sort", sort));

        verify(itemService).getItemsPage(eq(""), eq(sort), eq(1), eq(5));
    }

    @Test
    @DisplayName("GET /items - с пагинацией должен вернуть правильную страницу")
    void getItems_WithPagination_ShouldReturnCorrectPage() throws Exception {
        int pageNumber = 2;
        int pageSize = 10;
        List<List<ItemDto>> itemsGrid = new ArrayList<>();
        PagingInfo pagingInfo = new PagingInfo(pageNumber, pageSize, true, false, 5, 50);
        ItemsPageData pageData = new ItemsPageData(itemsGrid, "", "NO", pagingInfo);

        when(itemService.getItemsPage(eq(""), eq("NO"), eq(pageNumber), eq(pageSize))).thenReturn(pageData);

        mockMvc.perform(get("/items")
                        .param("pageNumber", String.valueOf(pageNumber))
                        .param("pageSize", String.valueOf(pageSize)))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("paging"));

        verify(itemService).getItemsPage(eq(""), eq("NO"), eq(pageNumber), eq(pageSize));
    }

    @Test
    @DisplayName("GET /items/{id} - должен вернуть страницу товара")
    void getItem_ShouldReturnItemPage() throws Exception {
        Long itemId = 1L;
        ItemDto itemDto = ItemDto.builder()
                .id(itemId)
                .title("Тестовый товар")
                .description("Описание товара")
                .imgPath("/images/test.jpg")
                .price(100L)
                .count(0)
                .build();

        when(itemService.getItemById(itemId)).thenReturn(itemDto);

        mockMvc.perform(get("/items/{id}", itemId))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("item", itemDto));

        verify(itemService).getItemById(itemId);
    }

    @Test
    @DisplayName("GET /items/{id} - при ненайденном товаре должен вернуть ошибку BAD_REQUEST")
    void getItem_WhenItemNotFound_ShouldReturnBadRequest() throws Exception {
        Long invalidItemId = 999L;

        when(itemService.getItemById(invalidItemId))
                .thenThrow(new ItemNotFoundException("Товар с ID " + invalidItemId + " не найден"));

        mockMvc.perform(get("/items/{id}", invalidItemId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Товар с ID " + invalidItemId + " не найден"));

        verify(itemService).getItemById(invalidItemId);
    }

    @Test
    @DisplayName("POST /items - действие PLUS должно увеличить количество товара и вернуть редирект")
    void updateCartItem_WithPlusAction_ShouldRedirect() throws Exception {
        Long itemId = 1L;
        String action = "PLUS";
        String redirectUrl = "redirect:/items?search=&sort=NO&pageNumber=1&pageSize=5";

        when(itemService.updateCartItemAndGetRedirectUrl(eq(itemId), eq(""), eq("NO"), eq(1), eq(5), eq(action)))
                .thenReturn(redirectUrl);

        mockMvc.perform(post("/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", action))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=&sort=NO&pageNumber=1&pageSize=5"));

        verify(itemService).updateCartItemAndGetRedirectUrl(eq(itemId), eq(""), eq("NO"), eq(1), eq(5), eq(action));
    }

    @Test
    @DisplayName("POST /items - действие MINUS должно уменьшить количество товара и вернуть редирект")
    void updateCartItem_WithMinusAction_ShouldRedirect() throws Exception {
        Long itemId = 1L;
        String action = "MINUS";
        String redirectUrl = "redirect:/items?search=тест&sort=ALPHA&pageNumber=2&pageSize=10";

        when(itemService.updateCartItemAndGetRedirectUrl(eq(itemId), eq("тест"), eq("ALPHA"), eq(2), eq(10), eq(action)))
                .thenReturn(redirectUrl);

        mockMvc.perform(post("/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", action)
                        .param("search", "тест")
                        .param("sort", "ALPHA")
                        .param("pageNumber", "2")
                        .param("pageSize", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=тест&sort=ALPHA&pageNumber=2&pageSize=10"));

        verify(itemService).updateCartItemAndGetRedirectUrl(eq(itemId), eq("тест"), eq("ALPHA"), eq(2), eq(10), eq(action));
    }

    @Test
    @DisplayName("POST /items - при ненайденном товаре должен вернуть ошибку BAD_REQUEST")
    void updateCartItem_WhenItemNotFound_ShouldReturnBadRequest() throws Exception {
        Long invalidItemId = 999L;
        String action = "PLUS";

        when(itemService.updateCartItemAndGetRedirectUrl(eq(invalidItemId), eq(""), eq("NO"), eq(1), eq(5), eq(action)))
                .thenThrow(new ItemNotFoundException("Товар с ID " + invalidItemId + " не найден"));

        mockMvc.perform(post("/items")
                        .param("id", String.valueOf(invalidItemId))
                        .param("action", action))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Товар с ID " + invalidItemId + " не найден"));

        verify(itemService).updateCartItemAndGetRedirectUrl(eq(invalidItemId), eq(""), eq("NO"), eq(1), eq(5), eq(action));
    }

    @Test
    @DisplayName("POST /items/{id} - действие PLUS должно увеличить количество товара и вернуть обновленную страницу")
    void updateCartItemFromItemPage_WithPlusAction_ShouldReturnUpdatedItemPage() throws Exception {
        Long itemId = 1L;
        String action = "PLUS";
        ItemDto updatedItem = ItemDto.builder()
                .id(itemId)
                .title("Тестовый товар")
                .description("Описание товара")
                .imgPath("/images/test.jpg")
                .price(100L)
                .count(2)
                .build();

        when(itemService.updateItemCountAndGetItem(itemId, action)).thenReturn(updatedItem);

        mockMvc.perform(post("/items/{id}", itemId)
                        .param("action", action))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("item", updatedItem));

        verify(itemService).updateItemCountAndGetItem(itemId, action);
    }

    @Test
    @DisplayName("POST /items/{id} - действие MINUS должно уменьшить количество товара и вернуть обновленную страницу")
    void updateCartItemFromItemPage_WithMinusAction_ShouldReturnUpdatedItemPage() throws Exception {
        Long itemId = 1L;
        String action = "MINUS";
        ItemDto updatedItem = ItemDto.builder()
                .id(itemId)
                .title("Тестовый товар")
                .description("Описание товара")
                .imgPath("/images/test.jpg")
                .price(100L)
                .count(1)
                .build();

        when(itemService.updateItemCountAndGetItem(itemId, action)).thenReturn(updatedItem);

        mockMvc.perform(post("/items/{id}", itemId)
                        .param("action", action))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attribute("item", updatedItem));

        verify(itemService).updateItemCountAndGetItem(itemId, action);
    }

    @Test
    @DisplayName("POST /items/{id} - при ненайденном товаре должен вернуть ошибку BAD_REQUEST")
    void updateCartItemFromItemPage_WhenItemNotFound_ShouldReturnBadRequest() throws Exception {
        Long invalidItemId = 999L;
        String action = "PLUS";

        when(itemService.updateItemCountAndGetItem(invalidItemId, action))
                .thenThrow(new ItemNotFoundException("Товар с ID " + invalidItemId + " не найден"));

        mockMvc.perform(post("/items/{id}", invalidItemId)
                        .param("action", action))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Товар с ID " + invalidItemId + " не найден"));

        verify(itemService).updateItemCountAndGetItem(invalidItemId, action);
    }

    @Test
    @DisplayName("POST /items/{id} - без параметра action должен вернуть ошибку валидации")
    void updateCartItemFromItemPage_WithoutActionParam_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/items/{id}", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /items - при ошибке сервиса должен вернуть INTERNAL_SERVER_ERROR")
    void getItems_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        when(itemService.getItemsPage(anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Ошибка подключения к базе данных"));

        mockMvc.perform(get("/items"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER"))
                .andExpect(jsonPath("$.message").value("Произошла непредвиденная ошибка"));
    }

    @Test
    @DisplayName("POST /items - без параметра id должен вернуть ошибку валидации")
    void updateCartItem_WithoutIdParam_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/items")
                        .param("action", "PLUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /items - без параметра action должен вернуть ошибку валидации")
    void updateCartItem_WithoutActionParam_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/items")
                        .param("id", "1"))
                .andExpect(status().isBadRequest());
    }
}