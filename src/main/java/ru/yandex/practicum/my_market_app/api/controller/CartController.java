package ru.yandex.practicum.my_market_app.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.my_market_app.core.model.CartItemDto;
import ru.yandex.practicum.my_market_app.core.service.CartService;
import ru.yandex.practicum.my_market_app.core.service.ItemService;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;

import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final ItemService itemService;

    @GetMapping("/items")
    public String getCartItems(Model model) {
        Long cartId = cartService.getCurrentCartId();

        List<CartItemDto> items = cartService.getCartItemsWithDetails(cartId);

        long total = cartService.getCartTotal(cartId);

        model.addAttribute("items", items);
        model.addAttribute("total", total);

        return "cart";
    }

    @PostMapping("/items")
    public String updateCartItem(
            @RequestParam Long id,
            @RequestParam String action,
            Model model) {
        Long cartId = cartService.getCurrentCartId();

        Item item = itemService.getItemEntityById(id);

        cartService.updateItemCount(cartId, item, action);

        List<CartItemDto> items = cartService.getCartItemsWithDetails(cartId);
        long total = cartService.getCartTotal(cartId);

        model.addAttribute("items", items);
        model.addAttribute("total", total);

        return "cart";
    }
}