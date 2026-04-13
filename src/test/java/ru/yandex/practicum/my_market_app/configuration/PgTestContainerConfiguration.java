package ru.yandex.practicum.my_market_app.configuration;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

@Profile("test")
@Configuration
public class PgTestContainerConfiguration {

    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("init.sql");

        POSTGRES_CONTAINER.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (POSTGRES_CONTAINER.isRunning()) {
                POSTGRES_CONTAINER.stop();
            }
        }));
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(POSTGRES_CONTAINER.getDriverClassName());
        dataSource.setUrl(POSTGRES_CONTAINER.getJdbcUrl());
        dataSource.setUsername(POSTGRES_CONTAINER.getUsername());
        dataSource.setPassword(POSTGRES_CONTAINER.getPassword());
        return dataSource;
    }

}
