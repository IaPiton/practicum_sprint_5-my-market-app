package ru.yandex.practicum.my_market_app.core.model;

import lombok.Builder;


@Builder
public record ItemDto(Long id, String title, String description, String imgPath, Long price, int count) {

      public static ItemDto createPlaceholder() {
            return ItemDto.builder()
                    .id(-1L)
                    .title("")
                    .description("")
                    .imgPath("")
                    .price(0L)
                    .count(0)
                    .build();
      }
}