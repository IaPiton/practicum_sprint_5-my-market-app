package ru.yandex.practicum.my_market_service.configuration;


import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestConfiguration.class)
@ActiveProfiles("test")
public abstract class TestcontainersTest {

    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        POSTGRES_CONTAINER.start();

        System.setProperty("spring.r2dbc.url",
                String.format("r2dbc:postgresql://%s:%d/%s",
                        POSTGRES_CONTAINER.getHost(),
                        POSTGRES_CONTAINER.getMappedPort(5432),
                        POSTGRES_CONTAINER.getDatabaseName()));
        System.setProperty("spring.r2dbc.username", POSTGRES_CONTAINER.getUsername());
        System.setProperty("spring.r2dbc.password", POSTGRES_CONTAINER.getPassword());
        System.setProperty("spring.sql.init.mode", "never");
        System.setProperty("spring.r2dbc.initialization-mode", "never");
    }

    static final RedisContainer REDIS_CONTAINER;

    static {
        REDIS_CONTAINER = new RedisContainer(DockerImageName.parse("redis:7.4.2-bookworm"))
                .withExposedPorts(6379)
                .withCommand("redis-server --port 6379");

        REDIS_CONTAINER.start();

         System.setProperty("spring.data.redis.port", String.valueOf(REDIS_CONTAINER.getMappedPort(6379)));
    }

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
    }
}