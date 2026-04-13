package ru.yandex.practicum.my_market_app.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_app.api.handler.ItemNotFoundException;
import ru.yandex.practicum.my_market_app.core.model.ItemDto;
import ru.yandex.practicum.my_market_app.core.model.ItemsPageData;
import ru.yandex.practicum.my_market_app.core.model.PagingInfo;
import ru.yandex.practicum.my_market_app.core.service.ItemService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@WebFluxTest(ItemController.class)
@DisplayName("Тесты контроллера товаров (WebFlux)")
class ItemControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @Test
    @DisplayName("GET / - должен вернуть главную страницу с товарами")
    void getItems_RootPath_ShouldReturnItemsPage() {
        String search = "";
        String sort = "NO";
        int pageNumber = 1;
        int pageSize = 5;

        List<ItemDto> items = Arrays.asList(
                ItemDto.builder()
                        .id(1L)
                        .title("Тестовый товар 1")
                        .description("Описание товара 1")
                        .imgPath("/images/test1.jpg")
                        .price(100L)
                        .build(),
                ItemDto.builder()
                        .id(2L)
                        .title("Тестовый товар 2")
                        .description("Описание товара 2")
                        .imgPath("/images/test2.jpg")
                        .price(150L)
                        .build()
        );

        List<List<ItemDto>> itemsGrid = List.of(
                Arrays.asList(items.get(0), items.get(1))
        );

        PagingInfo paging = new PagingInfo(1, 5, false, true, 3, 15);

        ItemsPageData itemsPageData = ItemsPageData.builder()
                .itemsGrid(itemsGrid)
                .search(search)
                .sort(sort)
                .paging(paging)
                .build();

        when(itemService.getItemsPage(eq(search), eq(sort), eq(pageNumber), eq(pageSize), anyString()))
                .thenReturn(Mono.just(itemsPageData));

        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Тестовый товар 1");
                    assert body.contains("Тестовый товар 2");
                });

        verify(itemService).getItemsPage(eq(search), eq(sort), eq(pageNumber), eq(pageSize), anyString());
    }

    @Test
    @DisplayName("GET /items - должен вернуть страницу с товарами")
    void getItems_ShouldReturnItemsPage() {
        String search = "тест";
        String sort = "price_asc";
        int pageNumber = 2;
        int pageSize = 10;

        List<ItemDto> items = List.of(
                ItemDto.builder()
                        .id(1L)
                        .title("Тестовый товар")
                        .description("Описание")
                        .price(100L)
                        .build()
        );

        List<List<ItemDto>> itemsGrid = List.of(items);

        PagingInfo paging = new PagingInfo(2, 10, true, true, 5, 50);

        ItemsPageData itemsPageData = ItemsPageData.builder()
                .itemsGrid(itemsGrid)
                .search(search)
                .sort(sort)
                .paging(paging)
                .build();

        when(itemService.getItemsPage(eq(search), eq(sort), eq(pageNumber), eq(pageSize), anyString()))
                .thenReturn(Mono.just(itemsPageData));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("search", search)
                        .queryParam("sort", sort)
                        .queryParam("pageNumber", pageNumber)
                        .queryParam("pageSize", pageSize)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Тестовый товар");
                });

        verify(itemService).getItemsPage(eq(search), eq(sort), eq(pageNumber), eq(pageSize), anyString());
    }

    @Test
    @DisplayName("GET /items - с параметрами по умолчанию должен вернуть страницу")
    void getItems_WithDefaultParams_ShouldReturnItemsPage() {
        String defaultSearch = "";
        String defaultSort = "NO";
        int defaultPageNumber = 1;
        int defaultPageSize = 5;

        List<ItemDto> items = List.of(
                ItemDto.builder().id(1L).title("Товар").price(100L).build()
        );

        List<List<ItemDto>> itemsGrid = List.of(items);

        PagingInfo paging = new PagingInfo(1, 5, false, false, 1, 1);

        ItemsPageData itemsPageData = ItemsPageData.builder()
                .itemsGrid(itemsGrid)
                .search(defaultSearch)
                .sort(defaultSort)
                .paging(paging)
                .build();

        when(itemService.getItemsPage(eq(defaultSearch), eq(defaultSort), eq(defaultPageNumber), eq(defaultPageSize), anyString()))
                .thenReturn(Mono.just(itemsPageData));

        webTestClient.get()
                .uri("/items")
                .exchange()
                .expectStatus().isOk();

        verify(itemService).getItemsPage(eq(defaultSearch), eq(defaultSort), eq(defaultPageNumber), eq(defaultPageSize), anyString());
    }

    @Test
    @DisplayName("GET /items - с пустой сеткой товаров должен вернуть пустую страницу")
    void getItems_WithEmptyGrid_ShouldReturnEmptyPage() {
        String search = "";
        String sort = "NO";
        int pageNumber = 1;
        int pageSize = 5;

        List<List<ItemDto>> emptyGrid = List.of();

        PagingInfo paging = new PagingInfo(1, 5, false, false, 0, 0);

        ItemsPageData itemsPageData = ItemsPageData.builder()
                .itemsGrid(emptyGrid)
                .search(search)
                .sort(sort)
                .paging(paging)
                .build();

        when(itemService.getItemsPage(eq(search), eq(sort), eq(pageNumber), eq(pageSize), anyString()))
                .thenReturn(Mono.just(itemsPageData));

        webTestClient.get()
                .uri("/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("0") || body.contains("пуст");
                });

        verify(itemService).getItemsPage(eq(search), eq(sort), eq(pageNumber), eq(pageSize), anyString());
    }

    @Test
    @DisplayName("GET /items/{id} - должен вернуть страницу товара")
    void getItem_ShouldReturnItemPage() {
        Long itemId = 1L;
        ItemDto item = ItemDto.builder()
                .id(itemId)
                .title("Тестовый товар")
                .description("Описание товара")
                .imgPath("/images/test.jpg")
                .price(100L)
                .build();

        when(itemService.getItemById(eq(itemId), anyString())).thenReturn(Mono.just(item));

        webTestClient.get()
                .uri("/items/{id}", itemId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Тестовый товар");
                    assert body.contains("100");
                });

        verify(itemService).getItemById(eq(itemId), anyString());
    }

    @Test
    @DisplayName("GET /items/{id} - при ненайденном товаре должен вернуть ошибку NOT_FOUND")
    void getItem_WhenItemNotFound_ShouldReturnNotFound() {
        Long invalidItemId = 999L;

        when(itemService.getItemById(eq(invalidItemId), anyString()))
                .thenReturn(Mono.error(new ItemNotFoundException("Товар с ID " + invalidItemId + " не найден")));

        webTestClient.get()
                .uri("/items/{id}", invalidItemId)
                .exchange()
                .expectStatus().isBadRequest();

        verify(itemService).getItemById(eq(invalidItemId), anyString());
    }

    @Test
    @DisplayName("POST /items - действие PLUS должно обновить корзину и вернуть редирект")
    void updateCartItem_WithPlusAction_ShouldUpdateCartAndRedirect() {
        Long itemId = 1L;
        String search = "тест";
        String sort = "price_asc";
        int pageNumber = 1;
        int pageSize = 10;
        String action = "PLUS";
        String redirectUrl = "redirect:/items?search=тест&sort=price_asc&pageNumber=1&pageSize=10";

        when(itemService.updateCartItemAndGetRedirectUrl(
                eq(itemId), eq(search), eq(sort), eq(pageNumber), eq(pageSize), eq(action), anyString()))
                .thenReturn(Mono.just(redirectUrl));

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId +
                        "&search=" + search +
                        "&sort=" + sort +
                        "&pageNumber=" + pageNumber +
                        "&pageSize=" + pageSize +
                        "&action=" + action)
                .exchange()
                .expectStatus().is3xxRedirection();

        verify(itemService).updateCartItemAndGetRedirectUrl(
                eq(itemId), eq(search), eq(sort), eq(pageNumber), eq(pageSize), eq(action), anyString());
    }

    @Test
    @DisplayName("POST /items - действие MINUS должно обновить корзину и вернуть редирект")
    void updateCartItem_WithMinusAction_ShouldUpdateCartAndRedirect() {
        Long itemId = 1L;
        String action = "MINUS";
        String redirectUrl = "redirect:/items";

        when(itemService.updateCartItemAndGetRedirectUrl(
                eq(itemId), anyString(), anyString(), anyInt(), anyInt(), eq(action), anyString()))
                .thenReturn(Mono.just(redirectUrl));

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=" + action)
                .exchange()
                .expectStatus().is3xxRedirection();

        verify(itemService).updateCartItemAndGetRedirectUrl(
                eq(itemId), anyString(), anyString(), anyInt(), anyInt(), eq(action), anyString());
    }

    @Test
    @DisplayName("POST /items - действие DELETE должно удалить товар из корзины и вернуть редирект")
    void updateCartItem_WithDeleteAction_ShouldRemoveFromCartAndRedirect() {
        Long itemId = 1L;
        String action = "DELETE";
        String redirectUrl = "redirect:/items";

        when(itemService.updateCartItemAndGetRedirectUrl(
                eq(itemId), anyString(), anyString(), anyInt(), anyInt(), eq(action), anyString()))
                .thenReturn(Mono.just(redirectUrl));

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=" + action)
                .exchange()
                .expectStatus().is3xxRedirection();

        verify(itemService).updateCartItemAndGetRedirectUrl(
                eq(itemId), anyString(), anyString(), anyInt(), anyInt(), eq(action), anyString());
    }

    @Test
    @DisplayName("POST /items - при ненайденном товаре должен вернуть ошибку NOT_FOUND")
    void updateCartItem_WhenItemNotFound_ShouldReturnNotFound() {
        Long invalidItemId = 999L;
        String action = "PLUS";

        when(itemService.updateCartItemAndGetRedirectUrl(
                eq(invalidItemId), anyString(), anyString(), anyInt(), anyInt(), eq(action), anyString()))
                .thenReturn(Mono.error(new ItemNotFoundException("Товар с ID " + invalidItemId + " не найден")));

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + invalidItemId + "&action=" + action)
                .exchange()
                .expectStatus().isBadRequest();

        verify(itemService).updateCartItemAndGetRedirectUrl(
                eq(invalidItemId), anyString(), anyString(), anyInt(), anyInt(), eq(action), anyString());
    }

    @Test
    @DisplayName("POST /items/{id} - действие PLUS должно обновить товар и вернуть страницу товара")
    void updateCartItemFromItemPage_WithPlusAction_ShouldUpdateItemAndReturnItemPage() {
        Long itemId = 1L;
        String action = "PLUS";

        ItemDto updatedItem = ItemDto.builder()
                .id(itemId)
                .title("Тестовый товар")
                .description("Описание")
                .price(100L)
                .count(3)
                .build();

        when(itemService.updateItemCountAndGetItem(eq(itemId), eq(action), anyString()))
                .thenReturn(Mono.just(updatedItem));

        webTestClient.post()
                .uri("/items/{id}", itemId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=" + action)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Тестовый товар");
                    assert body.contains("100");
                });

        verify(itemService).updateItemCountAndGetItem(eq(itemId), eq(action), anyString());
    }

    @Test
    @DisplayName("POST /items/{id} - действие MINUS должно обновить товар и вернуть страницу товара")
    void updateCartItemFromItemPage_WithMinusAction_ShouldUpdateItemAndReturnItemPage() {
        Long itemId = 1L;
        String action = "MINUS";

        ItemDto updatedItem = ItemDto.builder()
                .id(itemId)
                .title("Тестовый товар")
                .price(100L)
                .count(1)
                .build();

        when(itemService.updateItemCountAndGetItem(eq(itemId), eq(action), anyString()))
                .thenReturn(Mono.just(updatedItem));

        webTestClient.post()
                .uri("/items/{id}", itemId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=" + action)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("1");
                });

        verify(itemService).updateItemCountAndGetItem(eq(itemId), eq(action), anyString());
    }

    @Test
    @DisplayName("POST /items/{id} - при ненайденном товаре должен вернуть ошибку NOT_FOUND")
    void updateCartItemFromItemPage_WhenItemNotFound_ShouldReturnNotFound() {
        Long invalidItemId = 999L;
        String action = "PLUS";

        when(itemService.updateItemCountAndGetItem(eq(invalidItemId), eq(action), anyString()))
                .thenReturn(Mono.error(new ItemNotFoundException("Товар с ID " + invalidItemId + " не найден")));

        webTestClient.post()
                .uri("/items/{id}", invalidItemId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + invalidItemId + "&action=" + action)
                .exchange()
                .expectStatus().isBadRequest();

        verify(itemService).updateItemCountAndGetItem(eq(invalidItemId), eq(action), anyString());
    }

    @Test
    @DisplayName("POST /items/{id} - при неверном действии должен вернуть ошибку сервера")
    void updateCartItemFromItemPage_WithInvalidAction_ShouldReturnServerError() {
        Long itemId = 1L;
        String invalidAction = "INVALID_ACTION";

        when(itemService.updateItemCountAndGetItem(eq(itemId), eq(invalidAction), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Неизвестное действие: " + invalidAction)));

        webTestClient.post()
                .uri("/items/{id}", itemId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=" + invalidAction)
                .exchange()
                .expectStatus().is5xxServerError();

        verify(itemService).updateItemCountAndGetItem(eq(itemId), eq(invalidAction), anyString());
    }

    @Test
    @DisplayName("GET /items - при ошибке сервиса должен вернуть ошибку сервера")
    void getItems_WhenServiceThrowsException_ShouldReturnServerError() {
        when(itemService.getItemsPage(anyString(), anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Ошибка подключения к базе данных")));

        webTestClient.get()
                .uri("/items")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("GET /items/{id} - при ошибке сервиса должен вернуть ошибку сервера")
    void getItem_WhenServiceThrowsException_ShouldReturnServerError() {
        Long itemId = 1L;

        when(itemService.getItemById(eq(itemId), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Ошибка подключения к базе данных")));

        webTestClient.get()
                .uri("/items/{id}", itemId)
                .exchange()
                .expectStatus().is5xxServerError();
    }





    @Test
    @DisplayName("GET /items - с пагинацией должен корректно отображать информацию о страницах")
    void getItems_ShouldDisplayCorrectPagingInfo() {
        int pageNumber = 2;
        int pageSize = 5;

        PagingInfo paging = new PagingInfo(2, 5, true, true, 10, 50);

        ItemsPageData itemsPageData = ItemsPageData.builder()
                .itemsGrid(List.of())
                .search("")
                .sort("NO")
                .paging(paging)
                .build();

        when(itemService.getItemsPage(anyString(), anyString(), eq(pageNumber), eq(pageSize), anyString()))
                .thenReturn(Mono.just(itemsPageData));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("pageNumber", pageNumber)
                        .queryParam("pageSize", pageSize)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("2");
                    assert body.contains("10");
                    assert body.contains("50");
                });

        verify(itemService).getItemsPage(anyString(), anyString(), eq(pageNumber), eq(pageSize), anyString());
    }
}