package ru.yandex.practicum.my_market_service.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    private Long id;

    @Column(value = "username")
    private String username;

    @Column(value = "email")
    private String email;

    @Column(value = "password")
    private String password;

    @Column(value = "full_name")
    private String fullName;

    @Column(value = "phone")
    private String phone;

    @Column(value = "enabled")
    private Boolean enabled;

    @Column(value = "created_at")
    private LocalDateTime createdAt;

    @Column(value = "updated_at")
    private LocalDateTime updatedAt;
}