package ru.yandex.practicum.my_market_app.core.model;

public record PagingInfo(int pageNumber, int pageSize, boolean hasPrevious, boolean hasNext, int totalPages, long totalElements)  {
}