package ru.mai.lessons.rpks.controller;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
import ru.mai.lessons.rpks.model.Deduplication;
import ru.mai.lessons.rpks.model.Enrichment;
import ru.mai.lessons.rpks.model.Filter;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
@Testcontainers
@ContextConfiguration(initializers = {RulesTest.Initializer.class})
class RulesTest {
    static int containerPort = 5432;
    static int localPort = 5432;
    @Container
    static PostgreSQLContainer<?> postgreSQL = new PostgreSQLContainer<>(DockerImageName.parse("postgres"))
            .withDatabaseName("test_db")
            .withUsername("user")
            .withPassword("password")
            .withInitScript("init_script.sql")
            .withReuse(true)
            .withExposedPorts(containerPort)
            .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                    new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(localPort), new ExposedPort(containerPort)))
            ));

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgreSQL.getJdbcUrl(),
                    "spring.datasource.username=" + postgreSQL.getUsername(),
                    "spring.datasource.password=" + postgreSQL.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    @Setter
    protected String tableName;

    protected DataSource dataSource;

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        dataSource = Optional.ofNullable(dataSource).orElse(createConnectionPool());
    }

    @AfterEach
    void tearDown() {
        switch (tableName) {
            case "filter_rules" -> clearFilterTable();
            case "deduplication_rules" -> clearDeduplicationTable();
            case "enrichment_rules" -> clearEnrichmentTable();
        }
    }

    protected HikariDataSource createConnectionPool() {
        log.info("Create connection pool");
        var config = new HikariConfig();
        config.setJdbcUrl(postgreSQL.getJdbcUrl());
        config.setUsername(postgreSQL.getUsername());
        config.setPassword(postgreSQL.getPassword());
        config.setDriverClassName(postgreSQL.getDriverClassName());
        return new HikariDataSource(config);
    }

    protected void clearDeduplicationTable() {
        log.info("Clear table PostgreSQL: {}", tableName);
        try {
            DSLContext context = DSL.using(dataSource.getConnection(), SQLDialect.POSTGRES);
            context.deleteFrom(table(tableName)).execute();

            var result = context.select(
                            field("deduplication_id"),
                            field("rule_id"),
                            field("field_name"),
                            field("time_to_live_sec"),
                            field("is_active")
                    )
                    .from(table(tableName))
                    .fetch();

            context.alterSequence(tableName + "_id_seq").restart().execute();

            assertTrue(result.isEmpty());
        } catch (SQLException ex) {
            log.error("Error truncate table", ex);
        }
    }

    protected void clearEnrichmentTable() {
        log.info("Clear table PostgreSQL: {}", tableName);
        try {
            DSLContext context = DSL.using(dataSource.getConnection(), SQLDialect.POSTGRES);
            context.deleteFrom(table(tableName)).execute();

            var result = context.select(
                            field("enrichment_id"),
                            field("rule_id"),
                            field("field_name"),
                            field("field_name_enrichment"),
                            field("field_value"),
                            field("field_value_default")
                    )
                    .from(table(tableName))
                    .fetch();

            context.alterSequence(tableName + "_id_seq").restart().execute();

            assertTrue(result.isEmpty());
        } catch (SQLException ex) {
            log.error("Error truncate table", ex);
        }
    }

    protected void clearFilterTable() {
        log.info("Clear table PostgreSQL: {}", tableName);
        try {
            DSLContext context = DSL.using(dataSource.getConnection(), SQLDialect.POSTGRES);
            context.deleteFrom(table(tableName)).execute();

            var result = context.select(
                            field("filter_id"),
                            field("rule_id"),
                            field("field_name"),
                            field("filter_function_name"),
                            field("filter_value")
                    )
                    .from(table(tableName))
                    .fetch();

            context.alterSequence(tableName + "_id_seq").restart().execute();

            assertTrue(result.isEmpty());
        } catch (SQLException ex) {
            log.error("Error truncate table", ex);
        }
    }

    protected List<Deduplication> getDeduplicationRulesFromDB(Long deduplicationId, Long ruleId) {
        List<Deduplication> result = new ArrayList<>();
        try {
            log.info("Get deduplication rules");
            DSLContext context = DSL.using(dataSource.getConnection(), SQLDialect.POSTGRES);
            result = context.select(
                            field("id"),
                            field("deduplication_id"),
                            field("rule_id"),
                            field("field_name"),
                            field("time_to_live_sec"),
                            field("is_active")
                    )
                    .from(table(tableName))
                    .where(field("deduplication_id").eq(deduplicationId).and(field("rule_id").eq(ruleId)))
                    .fetch()
                    .map(r ->
                            new Deduplication(
                                    r.get("id", Long.class),
                                    r.get("deduplication_id", Long.class),
                                    r.get("rule_id", Long.class),
                                    r.get("field_name", String.class),
                                    r.get("time_to_live_sec", Long.class),
                                    r.get("is_active", Boolean.class))
                    );
        } catch (SQLException ex) {
            log.error("Error getting rule", ex);
        }
        return result;
    }

    protected List<Enrichment> getEnrichmentRulesFromDB(Long enrichmentId, Long ruleId) {
        List<Enrichment> result = new ArrayList<>();
        try {
            log.info("Get enrichment rules");
            DSLContext context = DSL.using(dataSource.getConnection(), SQLDialect.POSTGRES);
            result = context.select(
                            field("id"),
                            field("enrichment_id"),
                            field("rule_id"),
                            field("field_name"),
                            field("field_name_enrichment"),
                            field("field_value"),
                            field("field_value_default")
                    )
                    .from(table(tableName))
                    .where(field("enrichment_id").eq(enrichmentId).and(field("rule_id").eq(ruleId)))
                    .fetch()
                    .map(r -> new Enrichment(
                            r.get("id", Long.class),
                            r.get("enrichment_id", Long.class),
                            r.get("rule_id", Long.class),
                            r.get("field_name", String.class),
                            r.get("field_name_enrichment", String.class),
                            r.get("field_value", String.class),
                            r.get("field_value_default", String.class))
                    );

        } catch (SQLException ex) {
            log.error("Error getting rule", ex);
        }
        return result;
    }

    protected List<Filter> getFilterRulesFromDB(Long filterId, Long ruleId) {
        List<Filter> result = new ArrayList<>();
        try {
            log.info("Get filtering rules");
            DSLContext context = DSL.using(dataSource.getConnection(), SQLDialect.POSTGRES);
            result = context.select(
                            field("id"),
                            field("filter_id"),
                            field("rule_id"),
                            field("field_name"),
                            field("filter_function_name"),
                            field("filter_value")
                    )
                    .from(table(tableName))
                    .where(field("filter_id").eq(filterId).and(field("rule_id").eq(ruleId)))
                    .fetch()
                    .map(r -> new Filter(
                            r.get("id", Long.class),
                            r.get("filter_id", Long.class),
                            r.get("rule_id", Long.class),
                            r.get("field_name", String.class),
                            r.get("filter_function_name", String.class),
                            r.get("filter_value", String.class))
                    );
        } catch (SQLException ex) {
            log.error("Error creating rule", ex);
        }
        return result;
    }

    protected void createAndCheckDeduplicationRuleInPostgreSQL(Long deduplicationId, Long ruleId, String fieldName, Long timeToLiveSec, Boolean isActive) {
        try {
            log.info("Create deduplication rule");
            DSLContext context = DSL.using(dataSource.getConnection(), SQLDialect.POSTGRES);
            context.insertInto(table(tableName)).columns(
                            field("deduplication_id"),
                            field("rule_id"),
                            field("field_name"),
                            field("time_to_live_sec"),
                            field("is_active")
                    ).values(deduplicationId, ruleId, fieldName, timeToLiveSec, isActive)
                    .execute();

            log.info("Check rule from DB");
            var result = context.select(
                            field("deduplication_id"),
                            field("rule_id"),
                            field("field_name"),
                            field("time_to_live_sec"),
                            field("is_active")
                    )
                    .from(table(tableName))
                    .where(field("deduplication_id").eq(deduplicationId).and(field("rule_id").eq(ruleId)))
                    .fetch();

            String expectedValue =
                    String.format("deduplication_id,rule_id,field_name,time_to_live_sec,is_active\n%d,%d,%s,%d,%b\n",
                            deduplicationId, ruleId, fieldName, timeToLiveSec, isActive);

            assertEquals(expectedValue, result.formatCSV());
        } catch (SQLException ex) {
            log.error("Error creating rule", ex);
        }
    }

    protected void createAndCheckEnrichmentRuleInPostgreSQL(Long enrichmentId, Long ruleId, String fieldName, String fieldNameEnrichment, String fieldValue, String fieldValueDefault) {
        try {
            log.info("Create enrichment rule");
            DSLContext context = DSL.using(dataSource.getConnection(), SQLDialect.POSTGRES);
            context.insertInto(table(tableName)).columns(
                            field("enrichment_id"),
                            field("rule_id"),
                            field("field_name"),
                            field("field_name_enrichment"),
                            field("field_value"),
                            field("field_value_default")
                    ).values(enrichmentId, ruleId, fieldName, fieldNameEnrichment, fieldValue, fieldValueDefault)
                    .execute();

            log.info("Check rule from DB");
            var result = context.select(
                            field("enrichment_id"),
                            field("rule_id"),
                            field("field_name"),
                            field("field_name_enrichment"),
                            field("field_value"),
                            field("field_value_default")
                    )
                    .from(table(tableName))
                    .where(field("enrichment_id").eq(enrichmentId).and(field("rule_id").eq(ruleId)))
                    .fetch();

            String expectedValue =
                    String.format("enrichment_id,rule_id,field_name,field_name_enrichment,field_value,field_value_default\n%d,%d,%s,%s,%s,%s\n",
                            enrichmentId, ruleId, fieldName, fieldNameEnrichment, fieldValue, fieldValueDefault);

            assertEquals(expectedValue, result.formatCSV());
        } catch (SQLException ex) {
            log.error("Error creating rule", ex);
        }
    }

    protected void createAndCheckFilterRuleInPostgreSQL(Long filterId, Long ruleId, String fieldName, String filterFunctionName, String filterValue) {
        try {
            log.info("Create filtering rule");
            DSLContext context = DSL.using(dataSource.getConnection(), SQLDialect.POSTGRES);
            context.insertInto(table(tableName)).columns(
                            field("filter_id"),
                            field("rule_id"),
                            field("field_name"),
                            field("filter_function_name"),
                            field("filter_value")
                    ).values(filterId, ruleId, fieldName, filterFunctionName, filterValue)
                    .execute();

            log.info("Check rule from DB");
            var result = context.select(
                            field("filter_id"),
                            field("rule_id"),
                            field("field_name"),
                            field("filter_function_name"),
                            field("filter_value")
                    )
                    .from(table(tableName))
                    .where(field("filter_id").eq(filterId).and(field("rule_id").eq(ruleId)))
                    .fetch();

            String expectedValue =
                    String.format("filter_id,rule_id,field_name,filter_function_name,filter_value\n%d,%d,%s,%s,%s\n",
                            filterId, ruleId, fieldName, filterFunctionName, filterValue);

            assertEquals(expectedValue, result.formatCSV());
        } catch (SQLException ex) {
            log.error("Error creating rule", ex);
        }
    }

    protected String toJson(Object object) {
        String json = "{}";
        try {
            json = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error convert object to json", e);
        }
        return json;
    }
}
