package ru.yandex.practicum.my_market_service.core.security;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.persistence.model.UserDto;


public interface UserService {

    Mono<UserDto> findByName(String name);

}
