package ru.yandex.practicum.my_market_service.core.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.my_market_service.core.model.CartItemDto;
import ru.yandex.practicum.my_market_service.persistence.entity.CartItem;
import ru.yandex.practicum.my_market_service.persistence.entity.Item;
import ru.yandex.practicum.my_market_service.persistence.entity.User;
import ru.yandex.practicum.my_market_service.persistence.model.UserDto;

@Component
@RequiredArgsConstructor
public class UserMapper {

    public UserDto convertToUserDto(User user) {

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .enabled(user.getEnabled())
                .build();
    }
}