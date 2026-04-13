package ru.yandex.practicum.my_market_app.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
@RequiredArgsConstructor
public class ImageController {

    private static final String IMAGES_PATH = "static/images/";
    private static final String PLACEHOLDER_IMAGE = "static/images/placeholder.jpg";

    @GetMapping(value = "/images/{filename}", produces = MediaType.IMAGE_JPEG_VALUE)
    public Mono<ResponseEntity<?>> getImage(@PathVariable String filename) {
        return Mono.fromCallable(() -> {
            try {
                Resource resource = new ClassPathResource(IMAGES_PATH + filename);
                if (resource.exists() && resource.isReadable()) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.IMAGE_JPEG)
                            .body(resource);
                } else {
                    Resource placeholder = new ClassPathResource(PLACEHOLDER_IMAGE);
                    return ResponseEntity.ok()
                            .contentType(MediaType.IMAGE_JPEG)
                            .body(placeholder);
                }
            } catch (Exception e) {
                return ResponseEntity.notFound().build();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}