package ru.yandex.practicum.my_market_service.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
public class UserDetailsServiceImpl implements ReactiveUserDetailsService {

    private final UserService userService;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userService.findByName(username)
                .map(userEntity -> (UserDetails) new User(userEntity.getUsername(), userEntity.getPassword(), List.of(new SimpleGrantedAuthority("ROLE_USER"))))
                .switchIfEmpty(Mono.error(new UsernameNotFoundException(username)));
    }
}
