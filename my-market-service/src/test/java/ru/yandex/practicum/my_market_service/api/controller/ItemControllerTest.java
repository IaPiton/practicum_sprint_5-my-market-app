package ru.yandex.practicum.my_market_service.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.configuration.TestSecurityConfig;
import ru.yandex.practicum.my_market_service.core.model.ItemDto;
import ru.yandex.practicum.my_market_service.core.model.ItemsPageData;
import ru.yandex.practicum.my_market_service.core.model.PagingInfo;
import ru.yandex.practicum.my_market_service.core.service.ItemService;
import ru.yandex.practicum.my_market_service.core.security.SecurityService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(ItemController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Тесты контроллера товаров (WebFlux)")
class ItemControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private SecurityService securityService;

    @Test
    @DisplayName("GET / - должен вернуть главную страницу с товарами (доступно без авторизации)")
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

        when(itemService.getItemsPage(eq(search), any(), eq(pageNumber), eq(pageSize)))
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

        verify(itemService).getItemsPage(eq(search), any(), eq(pageNumber), eq(pageSize));
    }

    @Test
    @DisplayName("GET /items - должен вернуть страницу с товарами (доступно без авторизации)")
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

        when(itemService.getItemsPage(eq(search), any(), eq(pageNumber), eq(pageSize)))
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

        verify(itemService).getItemsPage(eq(search), any(), eq(pageNumber), eq(pageSize));
    }

    @Test
    @DisplayName("GET /items - с параметрами по умолчанию должен вернуть страницу (доступно без авторизации)")
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

        when(itemService.getItemsPage(eq(defaultSearch), any(), eq(defaultPageNumber), eq(defaultPageSize)))
                .thenReturn(Mono.just(itemsPageData));

        webTestClient.get()
                .uri("/items")
                .exchange()
                .expectStatus().isOk();

        verify(itemService).getItemsPage(eq(defaultSearch), any(), eq(defaultPageNumber), eq(defaultPageSize));
    }

    @Test
    @DisplayName("GET /items/{id} - должен вернуть страницу товара (доступно без авторизации)")
    void getItem_ShouldReturnItemPage() {
        Long itemId = 1L;
        ItemDto item = ItemDto.builder()
                .id(itemId)
                .title("Тестовый товар")
                .description("Описание товара")
                .imgPath("/images/test.jpg")
                .price(100L)
                .build();

        when(itemService.getItemById(eq(itemId))).thenReturn(Mono.just(item));

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

        verify(itemService).getItemById(eq(itemId));
    }

    @Test
    @DisplayName("POST /items - действие PLUS должно обновить корзину (требуется авторизация)")
    @WithMockUser(username = "user")
    void updateCartItem_WithPlusAction_ShouldUpdateCartAndRedirect() {
        Long itemId = 1L;
        String search = "тест";
        String sort = "price_asc";
        int pageNumber = 1;
        int pageSize = 10;
        String action = "PLUS";
        String redirectUrl = "redirect:/items?search=тест&sort=price_asc&pageNumber=1&pageSize=10";

        when(securityService.getCurrentUserId()).thenReturn(Mono.just(1L));
        when(itemService.updateCartItemAndGetRedirectUrl(
                eq(itemId), eq(search), eq(sort), eq(pageNumber), eq(pageSize), eq(action)))
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
                eq(itemId), eq(search), eq(sort), eq(pageNumber), eq(pageSize), eq(action));
    }

    @Test
    @DisplayName("POST /items - действие MINUS должно обновить корзину (требуется авторизация)")
    @WithMockUser(username = "user")
    void updateCartItem_WithMinusAction_ShouldUpdateCartAndRedirect() {
        Long itemId = 1L;
        String action = "MINUS";
        String redirectUrl = "redirect:/items";

        when(securityService.getCurrentUserId()).thenReturn(Mono.just(1L));
        when(itemService.updateCartItemAndGetRedirectUrl(
                eq(itemId), anyString(), anyString(), anyInt(), anyInt(), eq(action)))
                .thenReturn(Mono.just(redirectUrl));

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=" + action)
                .exchange()
                .expectStatus().is3xxRedirection();

        verify(itemService).updateCartItemAndGetRedirectUrl(
                eq(itemId), anyString(), anyString(), anyInt(), anyInt(), eq(action));
    }

    @Test
    @DisplayName("POST /items - действие DELETE должно удалить товар (требуется авторизация)")
    @WithMockUser(username = "user")
    void updateCartItem_WithDeleteAction_ShouldRemoveFromCartAndRedirect() {
        Long itemId = 1L;
        String action = "DELETE";
        String redirectUrl = "redirect:/items";

        when(securityService.getCurrentUserId()).thenReturn(Mono.just(1L));
        when(itemService.updateCartItemAndGetRedirectUrl(
                eq(itemId), anyString(), anyString(), anyInt(), anyInt(), eq(action)))
                .thenReturn(Mono.just(redirectUrl));

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=" + action)
                .exchange()
                .expectStatus().is3xxRedirection();

        verify(itemService).updateCartItemAndGetRedirectUrl(
                eq(itemId), anyString(), anyString(), anyInt(), anyInt(), eq(action));
    }

    @Test
    @DisplayName("POST /items - без авторизации должен вернуть ошибку UNAUTHORIZED")
    void updateCartItem_WithoutAuth_ShouldReturnUnauthorized() {
        long itemId = 1L;
        String action = "PLUS";

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=" + action)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /items/{id} - действие PLUS должно обновить товар (требуется авторизация)")
    @WithMockUser(username = "user")
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

        when(securityService.getCurrentUserId()).thenReturn(Mono.just(1L));
        when(itemService.updateItemCountAndGetItem(eq(itemId), eq(action)))
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

        verify(itemService).updateItemCountAndGetItem(eq(itemId), eq(action));
    }

    @Test
    @DisplayName("POST /items/{id} - без авторизации должен вернуть ошибку UNAUTHORIZED")
    void updateCartItemFromItemPage_WithoutAuth_ShouldReturnUnauthorized() {
        Long itemId = 1L;
        String action = "PLUS";

        when(securityService.getCurrentUserId())
                .thenReturn(Mono.error(new IllegalStateException("Пользователь не аутентифицирован")));

        webTestClient.post()
                .uri("/items/{id}", itemId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=" + action)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}