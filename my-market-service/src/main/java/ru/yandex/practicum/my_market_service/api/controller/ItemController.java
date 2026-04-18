package ru.yandex.practicum.my_market_service.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.api.model.ItemUpdateRequest;
import ru.yandex.practicum.my_market_service.core.service.ItemService;

import java.security.Principal;


@Controller
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @GetMapping({"/", "/items"})
    public Mono<String> getItems(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") int pageNumber,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            @AuthenticationPrincipal Mono<Principal> principal,
            Model model) {

        return principal
                .map(p -> true)
                .defaultIfEmpty(false)
                .flatMap(isAuth -> {
                    model.addAttribute("isAuthenticated", isAuth);
                    return itemService.getItemsPage(search, sort, pageNumber, pageSize);
                })
                .doOnNext(pageData -> {
                    model.addAttribute("items", pageData.getItemsGrid());
                    model.addAttribute("search", pageData.getSearch());
                    model.addAttribute("sort", pageData.getSort());
                    model.addAttribute("paging", pageData.getPaging());
                })
                .thenReturn("items");
    }

    @GetMapping("/items/{id}")
    public Mono<String> getItem(@PathVariable Long id,
                                Model model) {

        return itemService.getItemById(id)
                .doOnNext(item -> model.addAttribute("item", item))
                .thenReturn("item");
    }

    @PostMapping("/items")
    public Mono<String> updateCartItem(@ModelAttribute ItemUpdateRequest request) {
        return itemService.updateCartItemAndGetRedirectUrl(
                request.getId(),
                request.getSearch(),
                request.getSort(),
                request.getPageNumber(),
                request.getPageSize(),
                request.getAction()
        );
    }

    @PostMapping("/items/{id}")
    public Mono<String> updateCartItemFromItemPage(
            @ModelAttribute ItemUpdateRequest request,
            Model model) {

        return itemService.updateItemCountAndGetItem(request.getId(), request.getAction())
                .doOnNext(updatedItem -> model.addAttribute("item", updatedItem))
                .thenReturn("item");
    }
}