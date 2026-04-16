package ru.yandex.practicum.my_market_service.configuration;


import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(R2dbcTestConfiguration.class)
@ActiveProfiles("test")
public abstract class PostgresTestcontainersTest {

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
}