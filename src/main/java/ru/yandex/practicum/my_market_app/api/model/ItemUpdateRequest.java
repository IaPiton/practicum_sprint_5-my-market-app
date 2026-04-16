package ru.yandex.practicum.my_market_app.api.model;

import lombok.Data;

@Data
public class ItemUpdateRequest {
    private Long id;
    private String search = "";
    private String sort = "NO";
    private int pageNumber = 1;
    private int pageSize = 5;
    private String action;
}