package ru.yandex.practicum.my_market_service.core.model;

import org.springframework.data.domain.Sort;

public enum SortType {
    NO, ALPHA, PRICE;
    public static Sort toSort(SortType sortType) {
        if (sortType == null) {
            return Sort.unsorted();
        }

        return switch (sortType) {
            case ALPHA -> Sort.by(Sort.Direction.ASC, "title");
            case PRICE -> Sort.by(Sort.Direction.ASC, "price");
            default -> Sort.unsorted();
        };
    }
}