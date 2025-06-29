package ru.mai.lessons.rpks.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
@Testcontainers
@ContextConfiguration(initializers = {MetricsEndpointTest.Initializer.class})
class MetricsEndpointTest {

    @Container
    static final PostgreSQLContainer<?> postgreSQL = new PostgreSQLContainer<>(DockerImageName.parse("postgres"))
            .withDatabaseName("test_db")
            .withUsername("user")
            .withPassword("password");

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgreSQL.getJdbcUrl(),
                    "spring.datasource.username=" + postgreSQL.getUsername(),
                    "spring.datasource.password=" + postgreSQL.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }
    @Autowired
    protected MockMvc mockMvc;
    @Test
    void checkMetricsEndpoint() throws Exception {
        this.mockMvc.perform(get("/actuator/metrics"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void checkInfoEndpoint() throws Exception {
        this.mockMvc.perform(get("/actuator/info"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("countFilters")))
                .andExpect(content().string(containsString("countDeduplications")))
                .andExpect(content().string(containsString("countEnrichments")));
    }
}