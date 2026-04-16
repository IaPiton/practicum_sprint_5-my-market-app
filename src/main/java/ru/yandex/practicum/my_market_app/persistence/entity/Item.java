package ru.yandex.practicum.my_market_app.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "items")
public final class Item {

    @Id
    private Long id;

    @Column(value = "title")
    private String title;

    @Column(value = "description")
    private String description;

    @Column(value = "img_path")
    private String imgPath;

    @Column(value = "price")
    private Long price;

    @Column(value = "created_at")
    private LocalDateTime createdAt;

    @Column(value = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private int count;
}