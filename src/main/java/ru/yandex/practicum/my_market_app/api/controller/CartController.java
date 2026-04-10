package ru.yandex.practicum.my_market_app.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.yandex.practicum.my_market_app.core.model.CartItemDto;
import ru.yandex.practicum.my_market_app.core.service.CartService;

import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/items")
    public String getCartItems(Model model) {
        Long cartId = cartService.getCurrentCartId();

        List<CartItemDto> items = cartService.getCartItemsWithDetails(cartId);

        long total = cartService.getCartTotal(cartId);

        model.addAttribute("items", items);
        model.addAttribute("total", total);

        return "cart";
    }
}