package ru.yandex.practicum.my_market_app.core.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.my_market_app.core.model.ItemDto;

import java.util.ArrayList;
import java.util.List;

@Component
public class ItemsGridBuilder {

    public List<List<ItemDto>> buildGrid(List<ItemDto> items, int chunkSize) {
        List<List<ItemDto>> grid = new ArrayList<>();

        for (int i = 0; i < items.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, items.size());
            List<ItemDto> row = new ArrayList<>(items.subList(i, end));

            while (row.size() < chunkSize) {
                row.add(ItemDto.createPlaceholder());
            }

            grid.add(row);
        }

        if (grid.isEmpty()) {
            grid.add(createEmptyRow(chunkSize));
        }

        return grid;
    }

    private List<ItemDto> createEmptyRow(int chunkSize) {
        List<ItemDto> emptyRow = new ArrayList<>();
        for (int i = 0; i < chunkSize; i++) {
            emptyRow.add(ItemDto.createPlaceholder());
        }
        return emptyRow;
    }
}