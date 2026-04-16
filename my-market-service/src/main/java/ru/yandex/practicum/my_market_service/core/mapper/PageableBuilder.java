package ru.yandex.practicum.my_market_service.core.mapper;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PageableBuilder {

    private static final String SORT_ALPHA = "ALPHA";
    private static final String SORT_PRICE = "PRICE";
    private static final String SORT_TITLE_FIELD = "title";
    private static final String SORT_PRICE_FIELD = "price";

    public Pageable createPageable(int pageNumber, int pageSize, String sort) {
        int springPageNumber = Math.max(0, pageNumber - 1);
        Sort sortOrder = buildSort(sort);
        return PageRequest.of(springPageNumber, pageSize, sortOrder);
    }

    private Sort buildSort(String sort) {
        if (SORT_ALPHA.equalsIgnoreCase(sort)) {
            return Sort.by(SORT_TITLE_FIELD).ascending();
        } else if (SORT_PRICE.equalsIgnoreCase(sort)) {
            return Sort.by(SORT_PRICE_FIELD).ascending();
        }
        return Sort.unsorted();
    }
}