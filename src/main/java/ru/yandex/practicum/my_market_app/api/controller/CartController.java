package ru.yandex.practicum.my_market_app.api.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
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
    public String getCartItems(Model model,
                               HttpSession session) {
        Long cartId = cartService.getCurrentCartId(session.getId());

        List<CartItemDto> items = cartService.getCartItemsWithDetails(cartId);

        long total = cartService.getCartTotal(cartId);

        model.addAttribute("items", items);
        model.addAttribute("total", total);

        return "cart";
    }

    @PostMapping("/items")
    public String updateCartItem(
            HttpSession session,
            @RequestParam Long id,
            @RequestParam String action,
            Model model) {
        Long cartId = cartService.getCurrentCartId(session.getId());

        Item item = itemService.getItemEntityById(id);

        cartService.updateItemCount(cartId, item, action);

        List<CartItemDto> items = cartService.getCartItemsWithDetails(cartId);
        long total = cartService.getCartTotal(cartId);

        model.addAttribute("items", items);
        model.addAttribute("total", total);

        return "cart";
    }
}