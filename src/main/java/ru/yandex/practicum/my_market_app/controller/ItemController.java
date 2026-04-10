package ru.yandex.practicum.my_market_app.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ItemController {
    @GetMapping({"/", "/items"})
    public String getItems(Model model) {
        // Временные тестовые данные
        model.addAttribute("items", List.of());
        model.addAttribute("search", "");
        model.addAttribute("sort", "NO");
        model.addAttribute("paging", new PagingInfo(1, 5, false, false));

        return "items";
    }

    public record PagingInfo(
            int pageNumber,
            int pageSize,
            boolean hasPrevious,
            boolean hasNext
    ) {}
}
