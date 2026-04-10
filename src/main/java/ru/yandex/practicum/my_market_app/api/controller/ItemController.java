package ru.yandex.practicum.my_market_app.api.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.my_market_app.core.model.ItemsPageData;
import ru.yandex.practicum.my_market_app.core.service.ItemService;
import ru.yandex.practicum.my_market_app.persistence.model.ItemDto;


@Controller
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @GetMapping({"/", "/items"})
    public String getItems(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") int pageNumber,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            Model model) {
        ItemsPageData pageData = itemService.getItemsPage(
                search, sort, pageNumber, pageSize
        );

        model.addAttribute("items", pageData.getItemsGrid());
        model.addAttribute("search", pageData.getSearch());
        model.addAttribute("sort", pageData.getSort());
        model.addAttribute("paging", pageData.getPaging());

        return "items";
    }

    @GetMapping("/items/{id}")
    public String getItem(
            @PathVariable Long id,
            Model model) {
        ItemDto item = itemService.getItemById(id);
        model.addAttribute("item", item);
        return "item";
    }

    @PostMapping("/items")
    public String updateCartItem(
            @RequestParam Long id,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") int pageNumber,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            @RequestParam String action) {
        return itemService.updateCartItemAndGetRedirectUrl(id, search, sort, pageNumber, pageSize, action);
    }


}