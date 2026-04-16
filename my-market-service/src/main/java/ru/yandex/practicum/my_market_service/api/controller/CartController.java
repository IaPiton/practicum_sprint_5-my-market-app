package ru.yandex.practicum.my_market_service.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.api.model.ItemUpdateRequest;
import ru.yandex.practicum.my_market_service.core.service.CartService;
import ru.yandex.practicum.my_market_service.core.service.ItemService;


@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final ItemService itemService;

    @GetMapping("/items")
    public Mono<String> getCartItems(Model model,
                                     WebSession session) {
        return cartService.getBalance()
                .doOnNext(balance -> model.addAttribute("balance", balance))
                .then(cartService.getCurrentCartId(session.getId())
                        .flatMap(cartId ->
                                cartService.getCartItemsWithDetails(cartId)
                                        .collectList()
                                        .zipWith(cartService.getCartTotal(cartId))
                        )
                        .map(tuple -> {
                            model.addAttribute("items", tuple.getT1());
                            model.addAttribute("total", tuple.getT2());
                            return "cart";
                        }));
    }

    @PostMapping("/items")
    public Mono<String> updateCartItem(
            WebSession session,
            @ModelAttribute ItemUpdateRequest request,
            Model model) {
        return cartService.getBalance()
                .doOnNext(balance -> model.addAttribute("balance", balance))
                .then(cartService.getCurrentCartId(session.getId())
                        .flatMap(cartId ->
                                itemService.getItemEntityById(request.getId())
                                        .flatMap(item ->
                                                cartService.updateItemCount(cartId, item, request.getAction())
                                                        .then(Mono.zip(
                                                                cartService.getCartItemsWithDetails(cartId).collectList(),
                                                                cartService.getCartTotal(cartId)
                                                        )))).doOnNext(tuple -> {
                            model.addAttribute("items", tuple.getT1());
                            model.addAttribute("total", tuple.getT2());
                        })
                        .thenReturn("cart"));

    }
}