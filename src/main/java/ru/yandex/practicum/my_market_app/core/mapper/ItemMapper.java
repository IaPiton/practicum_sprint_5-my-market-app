package ru.yandex.practicum.my_market_app.core.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.my_market_app.persistence.entity.Item;
import ru.yandex.practicum.my_market_app.persistence.model.ItemDto;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ItemMapper {

    public ItemDto toDto(Item item, int countInCart) {
        return ItemDto.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .imgPath(item.getImgPath())
                .price(item.getPrice())
                .count(countInCart)
                .build();
    }

    public List<ItemDto> toDtoList(List<Item> items, Map<Long, Integer> cartItemCounts) {
        return items.stream()
                .map(item -> {
                    int countInCart = cartItemCounts.getOrDefault(item.getId(), 0);
                    return toDto(item, countInCart);
                })
                .toList();
    }
}