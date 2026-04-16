package ru.yandex.practicum.my_market_service.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.api.handler.PaymentFailedException;
import ru.yandex.practicum.my_market_service.core.service.CartService;
import ru.yandex.practicum.my_market_service.core.service.OrderService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping()
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    @PostMapping("/buy")
    public Mono<String> buy(WebSession session) {
        return cartService.getCurrentCartId(session.getId())
                .flatMap(orderService::createOrderFromCart)
                .map(order -> {
                    session.getAttributes().remove("paymentError");
                    return "redirect:/orders/" + order.getId() + "?newOrder=true";
                })
                .onErrorResume(PaymentFailedException.class, error -> {
                    String encodedError = URLEncoder.encode(error.getMessage(), StandardCharsets.UTF_8);
                    return Mono.just("redirect:/cart/items?paymentError=" + encodedError);
                });
    }

    @GetMapping("/orders/{id}")
    public Mono<String> getOrder(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean newOrder,
            Model model) {

        return orderService.getOrderById(id).doOnNext(
                order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("newOrder", newOrder);
                }
        ).thenReturn("order");
    }

    @GetMapping("/orders")
    public Mono<String> getOrders(Model model) {
        return  orderService.getAllOrders()
                .collectList()
                .doOnNext(
                        orders -> model.addAttribute("orders", orders)
                ).thenReturn("orders");
    }
}