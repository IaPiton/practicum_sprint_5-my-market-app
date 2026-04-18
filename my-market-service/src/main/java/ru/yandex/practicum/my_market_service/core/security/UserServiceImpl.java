package ru.yandex.practicum.my_market_service.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_service.core.mapper.UserMapper;
import ru.yandex.practicum.my_market_service.persistence.model.UserDto;
import ru.yandex.practicum.my_market_service.persistence.repository.UserRepository;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository repository;

    @Override
    public Mono<UserDto> findByName(String username) {
        return repository.findByUsername(username)
                .map(userMapper::convertToUserDto);
    }
}
