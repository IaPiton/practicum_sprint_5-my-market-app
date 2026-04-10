package api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.yandex.practicum.my_market_app.Application;
import ru.yandex.practicum.my_market_app.api.controller.ImageController;
import ru.yandex.practicum.my_market_app.api.handler.ApiExceptionHandler;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImageController.class)
@ContextConfiguration(classes = {Application.class, ApiExceptionHandler.class})
@DisplayName("Тесты контроллера изображений")
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /images/{filename} - должен вернуть существующее изображение")
    void getImage_WhenFileExists_ShouldReturnImage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/images/test.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG_VALUE));
    }

    @Test
    @DisplayName("GET /images/{filename} - при отсутствии изображения должен вернуть заглушку")
    void getImage_WhenFileNotFound_ShouldReturnPlaceholder() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/images/nonexistent.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG_VALUE));
    }

    @Test
    @DisplayName("GET /images/{filename} - должен корректно обрабатывать PNG изображения")
    void getImage_WithPngFile_ShouldReturnPngImage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/images/test.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG_VALUE));
    }

    @Test
    @DisplayName("GET /images/{filename} - при длинном имени файла должен корректно обработать")
    void getImage_WithLongFilename_ShouldHandleCorrectly() throws Exception {
        String longFilename = "very_long_filename_that_exceeds_normal_length_" +
                "with_many_characters_to_test_boundary_conditions.jpg";

        mockMvc.perform(MockMvcRequestBuilders.get("/images/" + longFilename))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG_VALUE));
    }

}