package ru.yandex.practicum.my_market_service.persistence.model;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String phone;
    private Boolean enabled;
}
