package ru.yandex.practicum.my_market_app.api.controller;


import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.my_market_app.core.model.ItemsPageData;
import ru.yandex.practicum.my_market_app.core.service.ItemService;
import ru.yandex.practicum.my_market_app.core.model.ItemDto;


@Controller
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @GetMapping({"/", "/items"})
    public String getItems(
            HttpSession session,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") int pageNumber,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            Model model) {
        ItemsPageData pageData = itemService.getItemsPage(
                search, sort, pageNumber, pageSize, session.getId()
        );

        model.addAttribute("items", pageData.getItemsGrid());
        model.addAttribute("search", pageData.getSearch());
        model.addAttribute("sort", pageData.getSort());
        model.addAttribute("paging", pageData.getPaging());

        return "items";
    }

    @GetMapping("/items/{id}")
    public String getItem(
            HttpSession session,
            @PathVariable Long id,
            Model model) {
        ItemDto item = itemService.getItemById(id, session.getId());
        model.addAttribute("item", item);
        return "item";
    }

    @PostMapping("/items")
    public String updateCartItem(
            HttpSession session,
            @RequestParam Long id,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") int pageNumber,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            @RequestParam String action) {
        return itemService.updateCartItemAndGetRedirectUrl(id, search, sort, pageNumber, pageSize, action, session.getId());
    }

    @PostMapping("/items/{id}")
    public String updateCartItemFromItemPage(
            HttpSession session,
            @PathVariable Long id,
            @RequestParam String action,
            Model model) {

        ItemDto updatedItem = itemService.updateItemCountAndGetItem(id, action, session.getId());

        model.addAttribute("item", updatedItem);

        return "item";
    }

}