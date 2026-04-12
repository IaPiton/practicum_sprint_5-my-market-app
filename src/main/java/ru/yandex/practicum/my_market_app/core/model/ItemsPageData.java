package ru.yandex.practicum.my_market_app.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemsPageData {
    private  List<List<ItemDto>> itemsGrid;
    private  String search;
    private  String sort;
    private  PagingInfo paging;
}
