package ru.yandex.practicum.my_market_app.core.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemsPageData {
    private  List<List<ItemDto>> itemsGrid;
    private  String search;
    private  String sort;
    private  PagingInfo paging;
}
