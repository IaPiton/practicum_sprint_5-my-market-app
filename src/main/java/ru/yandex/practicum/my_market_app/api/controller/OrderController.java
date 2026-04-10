package ru.yandex.practicum.my_market_app.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.my_market_app.core.model.OrderDto;
import ru.yandex.practicum.my_market_app.core.service.CartService;
import ru.yandex.practicum.my_market_app.core.service.OrderService;
import ru.yandex.practicum.my_market_app.persistence.entity.Order;

import java.util.List;

@Controller
@RequestMapping()
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    @PostMapping("/buy")
    public String buy() {
        Long cartId = cartService.getCurrentCartId();

        Order order = orderService.createOrderFromCart(cartId);

        return "redirect:/orders/" + order.getId() + "?newOrder=true";
    }

    @GetMapping("/orders/{id}")
    public String getOrder(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean newOrder,
            Model model) {
        OrderDto order = orderService.getOrderById(id);

        model.addAttribute("order", order);
        model.addAttribute("newOrder", newOrder);

        return "order";
    }

    @GetMapping("/orders")
    public String getOrders(Model model) {
        List<OrderDto> orders = orderService.getAllOrders();

        model.addAttribute("orders", orders);

        return "orders";
    }
}