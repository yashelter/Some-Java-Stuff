package ru.mai.lessons.rpks;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;
import ru.mai.lessons.rpks.impl.ServiceFiltering;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@Testcontainers
class ServiceTest {

    @Container
    private final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"));

    @Container
    private final JdbcDatabaseContainer<?> postgreSQL = new PostgreSQLContainer(DockerImageName.parse("postgres"))
            .withDatabaseName("test_db")
            .withUsername("user")
            .withPassword("password")
            .withInitScript("init_script.sql");

    ExecutorService executorForTest = Executors.newFixedThreadPool(2);

    private DataSource dataSource;
    private final String topicIn = "test_topic_in";
    private final String topicOut = "test_topic_out";
    private final short replicaFactor = 1;
    private final int partitions = 3;

    private final String tableName = "filter_rules";

    private final Service serviceFiltering = new ServiceFiltering();

    /**
     * Проверяет готовность Kafka
     */
    @Test
    void testStartKafka() {
        assertTrue(kafka.isRunning());
    }

    /**
     * Проверяет готовность postgreSQL
     */
    @Test
    void testStartPostgreSQL() {
        assertTrue(postgreSQL.isRunning());
    }

    /**
     * Проверяет возможность читать и писать из Kafka
     */
    @Test
    void testKafkaWriteReadMessage() {
        log.info("Bootstrap.servers: {}", kafka.getBootstrapServers());
        List<NewTopic> topics = Stream.of(topicIn, topicOut)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        try (AdminClient adminClient = createAdminClient();
             KafkaConsumer<String, String> consumer = createConsumer();
             KafkaProducer<String, String> producer = createProducer()) {
            log.info("Creating topics");
            adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);
            log.info("Created topics");
            log.info("Sending message");
            producer.send(new ProducerRecord<>(topicIn, "testKey", "json")).get();
            log.info("Sent message");

            log.info("Consumer subscribe");
            consumer.subscribe(Collections.singletonList(topicIn));
            log.info("Consumer start reading");

            getConsumerRecordsOutputTopic(consumer, 10, 1);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Тест проверяет взможноть чтения данных из PostgreSQL
     */
    @Test
    void testPostgreSQLReadValues() {
        clearTable();
        createAndCheckRuleInPostgreSQL(0L, 0L, "test_field", "equals", "test_value");
    }

    /**
     * Тест проверяет следующее правило фильтрации: field equals value.
     * Выполняется вставка правила в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - одно из них подходит под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил фильтрации.
     */
    @Test
    void testServiceFilteringEquals() {
        List<NewTopic> topics = Stream.of(topicIn, topicOut)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        try (AdminClient adminClient = createAdminClient();
             KafkaConsumer<String, String> consumer = createConsumer();
             KafkaProducer<String, String> producer = createProducer()) {

            checkAndCreateRequiredTopics(adminClient, topics);

            consumer.subscribe(Collections.singletonList(topicOut));

            clearTable();
            createAndCheckRuleInPostgreSQL(1L, 1L, "name", "equals", "alex");

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            Set.of("{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":null, \"age\":18, \"sex\":\"M\"}",
                    "{\"age\":18, \"sex\":\"M\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            String expectedJson = "{\"name\":\"alex\", \"age\":18, \"sex\":\"M\"}";
            producer.send(new ProducerRecord<>(topicIn, "expected", expectedJson)).get();

            Future<ConsumerRecords<String, String>> result = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecords = result.get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(1, consumerRecords.count());

            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertEquals(expectedJson, consumerRecord.value());
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();
            clearTable();

        } catch (Exception e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет следующее правило фильтрации: field not_equals value.
     * Выполняется вставка правила в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - одно из них подходит под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил фильтрации.
     */
    @Test
    void testServiceFilteringNotEquals() {
        List<NewTopic> topics = Stream.of(topicIn, topicOut)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        try (AdminClient adminClient = createAdminClient();
             KafkaConsumer<String, String> consumer = createConsumer();
             KafkaProducer<String, String> producer = createProducer()) {

            clearTable();
            checkAndCreateRequiredTopics(adminClient, topics);

            consumer.subscribe(Collections.singletonList(topicOut));

            createAndCheckRuleInPostgreSQL(1L, 1L, "name", "not_equals", "alex");

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            Set.of("{\"name\":\"alex\", \"age\":19, \"sex\":\"M\"}",
                    "{\"name\":\"alex\", \"age\":null}",
                    "{\"name\":\"alex\", \"age\":, \"sex\":null}",
                    "{\"name\":\"alex\", \"sex\":\"M\"}",
                    "{\"name\":\"alex\"}").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            String expectedJson = "{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\"}";
            producer.send(new ProducerRecord<>(topicIn, "expected", expectedJson)).get();

            Future<ConsumerRecords<String, String>> result = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecords = result.get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(1, consumerRecords.count());

            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertEquals(expectedJson, consumerRecord.value());
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();
            clearTable();

        } catch (Exception e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет следующее правило фильтрации: field contains value.
     * Выполняется вставка правила в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - одно из них подходит под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил фильтрации.
     */
    @Test
    void testServiceFilteringContains() {
        List<NewTopic> topics = Stream.of(topicIn, topicOut)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        try (AdminClient adminClient = createAdminClient();
             KafkaConsumer<String, String> consumer = createConsumer();
             KafkaProducer<String, String> producer = createProducer()) {

            checkAndCreateRequiredTopics(adminClient, topics);

            consumer.subscribe(Collections.singletonList(topicOut));

            clearTable();
            createAndCheckRuleInPostgreSQL(1L, 1L, "name", "contains", "alexander");

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            Set.of("{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":null, \"age\":18, \"sex\":\"M\"}",
                    "{\"age\":18, \"sex\":\"M\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            String expectedJson = "{\"name\":\"alexander_pushkin\", \"age\":18, \"sex\":\"M\"}";
            producer.send(new ProducerRecord<>(topicIn, "expected", expectedJson)).get();

            Future<ConsumerRecords<String, String>> result = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecords = result.get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(1, consumerRecords.count());

            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertEquals(expectedJson, consumerRecord.value());
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();
            clearTable();

        } catch (Exception e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет следующее правило фильтрации: field not_contains value.
     * Выполняется вставка правила в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - одно из них подходит под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил фильтрации.
     */
    @Test
    void testServiceFilteringNotContains() {
        List<NewTopic> topics = Stream.of(topicIn, topicOut)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        try (AdminClient adminClient = createAdminClient();
             KafkaConsumer<String, String> consumer = createConsumer();
             KafkaProducer<String, String> producer = createProducer()) {

            checkAndCreateRequiredTopics(adminClient, topics);

            consumer.subscribe(Collections.singletonList(topicOut));

            clearTable();
            createAndCheckRuleInPostgreSQL(1L, 1L, "name", "not_contains", "alexander");

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            Set.of("{\"name\":\"alexander_pushkin\", \"age\":19, \"sex\":\"M\"}",
                    "{\"name\":\"alexander_pushkin\", \"age\":null}",
                    "{\"name\":\"alexander_pushkin\", \"age\":, \"sex\":null}",
                    "{\"name\":\"alexander_pushkin\", \"sex\":\"M\"}",
                    "{\"name\":\"alexander_pushkin\"}").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            String expectedJson = "{\"name\":\"alex_pushkin\", \"age\":18, \"sex\":\"M\"}";
            producer.send(new ProducerRecord<>(topicIn, "expected", expectedJson)).get();

            Future<ConsumerRecords<String, String>> result = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecords = result.get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(1, consumerRecords.count());

            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertEquals(expectedJson, consumerRecord.value());
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();
            clearTable();

        } catch (Exception e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет следующее правило фильтрации: field1 equals value1 and field2 not_equals value2
     * Выполняется вставка правил в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - одно из них подходит под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил фильтрации.
     */
    @Test
    void testServiceFilteringEqualsTwoRules() {
        List<NewTopic> topics = Stream.of(topicIn, topicOut)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        try (AdminClient adminClient = createAdminClient();
             KafkaConsumer<String, String> consumer = createConsumer();
             KafkaProducer<String, String> producer = createProducer()) {

            checkAndCreateRequiredTopics(adminClient, topics);

            consumer.subscribe(Collections.singletonList(topicOut));

            clearTable();
            createAndCheckRuleInPostgreSQL(1L, 1L, "name", "equals", "alex");
            createAndCheckRuleInPostgreSQL(1L, 2L, "age", "equals", "18");

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            Set.of("{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"\", \"age\":-, \"sex\":\"M\"}",
                    "{\"name\":null, \"age\":\"\", \"sex\":\"M\"}",
                    "{\"name\":null, \"age\":null, \"sex\":\"M\"}",
                    "{\"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"alex\", \"age\":19, \"sex\":\"F\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            String expectedJson = "{\"name\":\"alex\", \"age\":18, \"sex\":\"M\"}";
            producer.send(new ProducerRecord<>(topicIn, "expected", expectedJson)).get();

            Future<ConsumerRecords<String, String>> result = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecords = result.get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(1, consumerRecords.count());

            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertEquals(expectedJson, consumerRecord.value());
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();

            clearTable();
        } catch (Exception e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет следующее правило фильтрации: field1 equals value1 and field2 not_equals value2
     * Выполняется вставка правил в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - одно из них подходит под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил фильтрации.
     */
    @Test
    void testServiceFilteringEqualsNotEqualsTwoRules() {
        List<NewTopic> topics = Stream.of(topicIn, topicOut)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        try (AdminClient adminClient = createAdminClient();
             KafkaConsumer<String, String> consumer = createConsumer();
             KafkaProducer<String, String> producer = createProducer()) {

            checkAndCreateRequiredTopics(adminClient, topics);

            consumer.subscribe(Collections.singletonList(topicOut));

            clearTable();
            createAndCheckRuleInPostgreSQL(1L, 1L, "name", "equals", "alex");
            createAndCheckRuleInPostgreSQL(1L, 2L, "age", "not_equals", "18");

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            Set.of("{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"\", \"age\":-, \"sex\":\"M\"}",
                    "{\"name\":null, \"age\":\"\", \"sex\":\"M\"}",
                    "{\"name\":null, \"age\":null, \"sex\":\"M\"}",
                    "{\"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"alex\", \"age\":18, \"sex\":\"M\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            String expectedJson = "{\"name\":\"alex\", \"age\":20, \"sex\":\"M\"}";
            producer.send(new ProducerRecord<>(topicIn, "expected", expectedJson)).get();

            Future<ConsumerRecords<String, String>> result = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecords = result.get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(1, consumerRecords.count());

            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertEquals(expectedJson, consumerRecord.value());
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();

            clearTable();
        } catch (Exception e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет следующее правило фильтрации: field1 contains value1 and field2 contains value2
     * Выполняется вставка правил в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - одно из них подходит под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил фильтрации.
     */
    @Test
    void testServiceFilteringContainsTwoRules() {
        List<NewTopic> topics = Stream.of(topicIn, topicOut)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        try (AdminClient adminClient = createAdminClient();
             KafkaConsumer<String, String> consumer = createConsumer();
             KafkaProducer<String, String> producer = createProducer()) {

            checkAndCreateRequiredTopics(adminClient, topics);

            consumer.subscribe(Collections.singletonList(topicOut));

            clearTable();
            createAndCheckRuleInPostgreSQL(1L, 1L, "name", "contains", "alexander");
            createAndCheckRuleInPostgreSQL(1L, 2L, "name", "contains", "pushkin");

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            Set.of("{\"name\":\"alexander\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"pushkin\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":null, \"age\":18, \"sex\":\"M\"}",
                    "{\"age\":18, \"sex\":\"M\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            String expectedJson = "{\"name\":\"alexander_pushkin\", \"age\":18, \"sex\":\"M\"}";
            producer.send(new ProducerRecord<>(topicIn, "expected", expectedJson)).get();

            Future<ConsumerRecords<String, String>> result = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecords = result.get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(1, consumerRecords.count());

            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertEquals(expectedJson, consumerRecord.value());
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();
            clearTable();

        } catch (Exception e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет следующее правило фильтрации: field1 contains value1 and field2 not_contains value2
     * Выполняется вставка правил в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - одно из них подходит под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил фильтрации.
     */
    @Test
    void testServiceFilteringContainsNotContainsTwoRules() {
        List<NewTopic> topics = Stream.of(topicIn, topicOut)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        try (AdminClient adminClient = createAdminClient();
             KafkaConsumer<String, String> consumer = createConsumer();
             KafkaProducer<String, String> producer = createProducer()) {

            checkAndCreateRequiredTopics(adminClient, topics);

            consumer.subscribe(Collections.singletonList(topicOut));

            clearTable();
            createAndCheckRuleInPostgreSQL(1L, 1L, "name", "contains", "alexander");
            createAndCheckRuleInPostgreSQL(1L, 2L, "name", "not_contains", "ivanov");

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            Set.of("{\"name\":\"alexander_ivanov\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"pushkin\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"ivanov\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":null, \"age\":18, \"sex\":\"M\"}",
                    "{\"age\":18, \"sex\":\"M\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            String expectedJson = "{\"name\":\"alexander_pushkin\", \"age\":18, \"sex\":\"M\"}";
            producer.send(new ProducerRecord<>(topicIn, "expected", expectedJson)).get();

            Future<ConsumerRecords<String, String>> result = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecords = result.get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(1, consumerRecords.count());

            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertEquals(expectedJson, consumerRecord.value());
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();
            clearTable();

        } catch (Exception e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет следующее правило фильтрации: field1 equals value1 and field2 contains value2
     * Выполняется вставка правил в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - одно из них подходит под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил фильтрации.
     */
    @Test
    void testServiceFilteringEqualsContainsTwoRules() {
        List<NewTopic> topics = Stream.of(topicIn, topicOut)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        try (AdminClient adminClient = createAdminClient();
             KafkaConsumer<String, String> consumer = createConsumer();
             KafkaProducer<String, String> producer = createProducer()) {

            checkAndCreateRequiredTopics(adminClient, topics);

            consumer.subscribe(Collections.singletonList(topicOut));

            clearTable();
            createAndCheckRuleInPostgreSQL(1L, 1L, "age", "equals", "18");
            createAndCheckRuleInPostgreSQL(1L, 2L, "name", "contains", "alexander");

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            Set.of("{\"name\":\"alex_ivanov\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"pushkin\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"ivanov\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"alexander\", \"age\":19, \"sex\":\"M\"}",
                    "{\"name\":\"alexander\", \"age\":null, \"sex\":\"M\"}",
                    "{\"name\":\"alexander\", \"age\":, \"sex\":\"M\"}",
                    "{\"name\":\"\", \"age\":\"\", \"sex\":\"M\"}",
                    "{\"name\":null, \"age\":18, \"sex\":\"M\"}",
                    "{\"age\":18, \"sex\":\"M\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            String expectedJson = "{\"name\":\"alexander_pushkin\", \"age\":18, \"sex\":\"M\"}";
            producer.send(new ProducerRecord<>(topicIn, "expected", expectedJson)).get();

            Future<ConsumerRecords<String, String>> result = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecords = result.get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(1, consumerRecords.count());

            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertEquals(expectedJson, consumerRecord.value());
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();
            clearTable();

        } catch (Exception e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет следующее правило фильтрации: field1 equals value1 and field2 not_contains value2
     * Выполняется вставка правил в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - одно из них подходит под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил фильтрации.
     */
    @Test
    void testServiceFilteringEqualsNotContainsTwoRules() {
        List<NewTopic> topics = Stream.of(topicIn, topicOut)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        try (AdminClient adminClient = createAdminClient();
             KafkaConsumer<String, String> consumer = createConsumer();
             KafkaProducer<String, String> producer = createProducer()) {

            checkAndCreateRequiredTopics(adminClient, topics);

            consumer.subscribe(Collections.singletonList(topicOut));

            clearTable();
            createAndCheckRuleInPostgreSQL(1L, 1L, "age", "equals", "18");
            createAndCheckRuleInPostgreSQL(1L, 2L, "name", "not_contains", "ivanov");

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            Set.of("{\"name\":\"alex_ivanov\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"pushkin\", \"age\":19, \"sex\":\"M\"}",
                    "{\"name\":\"alexander\", \"age\":null, \"sex\":\"M\"}",
                    "{\"name\":\"alexander\", \"age\":, \"sex\":\"M\"}",
                    "{\"name\":\"\", \"age\":\"\", \"sex\":\"M\"}",
                    "{\"name\":null, \"sex\":\"M\"}",
                    "{\"age\":18, \"sex\":\"M\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            String expectedJson = "{\"name\":\"alexander_pushkin\", \"age\":18, \"sex\":\"M\"}";
            producer.send(new ProducerRecord<>(topicIn, "expected", expectedJson)).get();

            Future<ConsumerRecords<String, String>> result = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecords = result.get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(1, consumerRecords.count());

            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertEquals(expectedJson, consumerRecord.value());
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();
            clearTable();

        } catch (Exception e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет следующее правило фильтрации: field1 contains value1 and field2 not_equals value2
     * Выполняется вставка правил в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - одно из них подходит под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил фильтрации.
     */
    @Test
    void testServiceFilteringContainsNotEqualsTwoRules() {
        List<NewTopic> topics = Stream.of(topicIn, topicOut)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        try (AdminClient adminClient = createAdminClient();
             KafkaConsumer<String, String> consumer = createConsumer();
             KafkaProducer<String, String> producer = createProducer()) {

            checkAndCreateRequiredTopics(adminClient, topics);

            consumer.subscribe(Collections.singletonList(topicOut));

            clearTable();
            createAndCheckRuleInPostgreSQL(1L, 1L, "age", "not_equals", "20");
            createAndCheckRuleInPostgreSQL(1L, 2L, "name", "contains", "alexander");

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            Set.of("{\"name\":\"alex_ivanov\", \"age\":20, \"sex\":\"M\"}",
                    "{\"name\":\"pushkin\", \"age\":19, \"sex\":\"M\"}",
                    "{\"name\":\"alex\", \"age\":null, \"sex\":\"M\"}",
                    "{\"name\":\"alex\", \"age\":, \"sex\":\"M\"}",
                    "{\"name\":\"\", \"age\":\"\", \"sex\":\"M\"}",
                    "{\"name\":null, \"sex\":\"M\"}",
                    "{\"age\":18, \"sex\":\"M\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            String expectedJson = "{\"name\":\"alexander_pushkin\", \"age\":18, \"sex\":\"M\"}";
            producer.send(new ProducerRecord<>(topicIn, "expected", expectedJson)).get();

            Future<ConsumerRecords<String, String>> result = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecords = result.get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(1, consumerRecords.count());

            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertEquals(expectedJson, consumerRecord.value());
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();
            clearTable();

        } catch (Exception e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет, что приложение корректно работает без правил
     * Выполняется вставка правил в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - ни одно из них не подходит под правило.
     * Проверяется выходной топик - не должно быть сообщений на выходе
     */
    @Test
    void testServiceFilteringNotRules() {
        List<NewTopic> topics = Stream.of(topicIn, topicOut)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        try (AdminClient adminClient = createAdminClient();
             KafkaConsumer<String, String> consumer = createConsumer();
             KafkaProducer<String, String> producer = createProducer()) {

            checkAndCreateRequiredTopics(adminClient, topics);

            consumer.subscribe(Collections.singletonList(topicOut));

            clearTable();

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            Set.of("{\"name\":\"alex_ivanov\", \"age\":20, \"sex\":\"M\"}",
                    "{\"name\":\"pushkin\", \"age\":19, \"sex\":\"M\"}",
                    "{\"name\":\"alex\", \"age\":null, \"sex\":\"M\"}",
                    "{\"name\":\"alex\", \"age\":, \"sex\":\"M\"}",
                    "{\"name\":\"\", \"age\":\"\", \"sex\":\"M\"}",
                    "{\"name\":null, \"sex\":\"M\"}",
                    "{\"age\":18, \"sex\":\"M\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            Future<ConsumerRecords<String, String>> result = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecords = result.get(60, TimeUnit.SECONDS);

            assertTrue(consumerRecords.isEmpty());

            serviceIsWork.cancel(true);
            executorForTest.shutdown();
            clearTable();

        } catch (Exception e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет следующее правило фильтрации: field1 equals value1,
     * а затем обновляет правило фильтрации: field1 contains value2
     * Выполняется вставка правил в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - одно из них подходит под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил фильтрации для первого правила, а потом для обновленного
     */
    @Test
    void testServiceFilteringUpdateRule() {
        List<NewTopic> topics = Stream.of(topicIn, topicOut)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        try (AdminClient adminClient = createAdminClient();
             KafkaConsumer<String, String> consumer = createConsumer();
             KafkaProducer<String, String> producer = createProducer()) {

            checkAndCreateRequiredTopics(adminClient, topics);

            consumer.subscribe(Collections.singletonList(topicOut));

            clearTable();
            createAndCheckRuleInPostgreSQL(1L, 1L, "name", "equals", "alex");


            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            Set.of("{\"name\":\"alex_ivanov\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"pushkin\", \"age\":19, \"sex\":\"M\"}",
                    "{\"name\":\"alexander\", \"age\":null, \"sex\":\"M\"}",
                    "{\"name\":, \"age\":, \"sex\":\"M\"}",
                    "{\"name\":\"\", \"age\":\"\", \"sex\":\"M\"}",
                    "{\"name\":null, \"sex\":\"M\"}",
                    "{\"age\":17, \"sex\":\"M\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            String expectedJson = "{\"name\":\"alex\", \"age\":18, \"sex\":\"M\"}";
            producer.send(new ProducerRecord<>(topicIn, "expected", expectedJson)).get();

            Future<ConsumerRecords<String, String>> result = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecords = result.get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(1, consumerRecords.count());

            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertEquals(expectedJson, consumerRecord.value());
            }

            clearTable();
            createAndCheckRuleInPostgreSQL(1L, 1L, "name", "contains", "alexander");
            log.info("Wait until application updated rules from DB");
            Thread.sleep(config.getLong("application.updateIntervalSec") * 1000 * 2 + 1);

            Set.of("{\"name\":\"alex_ivanov\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"pushkin\", \"age\":19, \"sex\":\"M\"}",
                    "{\"name\":\"alex\", \"age\":null, \"sex\":\"M\"}",
                    "{\"name\":, \"age\":, \"sex\":\"M\"}",
                    "{\"name\":\"\", \"age\":\"\", \"sex\":\"M\"}",
                    "{\"name\":null, \"sex\":\"M\"}",
                    "{\"age\":17, \"sex\":\"M\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            String expectedJsonOther = "{\"name\":\"alexander\", \"age\":18, \"sex\":\"M\"}";
            producer.send(new ProducerRecord<>(topicIn, "expected", expectedJsonOther)).get();

            Future<ConsumerRecords<String, String>> resultOther = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecordsOther = resultOther.get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecordsOther.isEmpty());
            assertEquals(1, consumerRecordsOther.count());

            for (ConsumerRecord<String, String> consumerRecord : consumerRecordsOther) {
                assertNotNull(consumerRecord.value());
                assertEquals(expectedJsonOther, consumerRecord.value());
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();
            clearTable();

        } catch (Exception e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет следующее правило фильтрации: field1 contains value1,
     * а затем добавляет ещё одно правило фильтрации: field1 equals value2
     * Выполняется вставка правил в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - одно из них подходит под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил фильтрации для первого правила, а потом для обновленного
     */
    @Test
    void testServiceFilteringAddNewRule() {
        List<NewTopic> topics = Stream.of(topicIn, topicOut)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        try (AdminClient adminClient = createAdminClient();
             KafkaConsumer<String, String> consumer = createConsumer();
             KafkaProducer<String, String> producer = createProducer()) {

            checkAndCreateRequiredTopics(adminClient, topics);

            consumer.subscribe(Collections.singletonList(topicOut));

            clearTable();
            createAndCheckRuleInPostgreSQL(1L, 1L, "name", "contains", "alexander");

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            Set.of("{\"name\":\"alex_ivanov\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"pushkin\", \"age\":19, \"sex\":\"M\"}",
                    "{\"name\":\"alex\", \"age\":null, \"sex\":\"M\"}",
                    "{\"name\":, \"age\":, \"sex\":\"M\"}",
                    "{\"name\":\"\", \"age\":\"\", \"sex\":\"M\"}",
                    "{\"name\":null, \"sex\":\"M\"}",
                    "{\"age\":17, \"sex\":\"M\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            String expectedJson = "{\"name\":\"alexander\", \"age\":18, \"sex\":\"M\"}";
            producer.send(new ProducerRecord<>(topicIn, "expected", expectedJson)).get();

            Future<ConsumerRecords<String, String>> result = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecords = result.get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(1, consumerRecords.count());

            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertEquals(expectedJson, consumerRecord.value());
            }

            createAndCheckRuleInPostgreSQL(1L, 2L, "age", "equals", "18");
            log.info("Wait until application updated rules from DB");
            Thread.sleep(config.getLong("application.updateIntervalSec") * 1000 * 2 + 1);

            Set.of("{\"name\":\"alex_ivanov\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"pushkin\", \"age\":19, \"sex\":\"M\"}",
                    "{\"name\":\"alex\", \"age\":null, \"sex\":\"M\"}",
                    "{\"name\":, \"age\":, \"sex\":\"M\"}",
                    "{\"name\":\"\", \"age\":\"\", \"sex\":\"M\"}",
                    "{\"name\":null, \"sex\":\"M\"}",
                    "{\"age\":17, \"sex\":\"M\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            String expectedJsonOther = "{\"name\":\"alexander\", \"age\":18, \"sex\":\"M\"}";
            producer.send(new ProducerRecord<>(topicIn, "expected", expectedJsonOther)).get();

            Future<ConsumerRecords<String, String>> resultOther = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1));

            var consumerRecordsOther = resultOther.get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecordsOther.isEmpty());
            assertEquals(1, consumerRecordsOther.count());

            for (ConsumerRecord<String, String> consumerRecord : consumerRecordsOther) {
                assertNotNull(consumerRecord.value());
                assertEquals(expectedJsonOther, consumerRecord.value());
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();
            clearTable();

        } catch (Exception e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    private AdminClient createAdminClient() {
        return AdminClient.create(ImmutableMap.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()));
    }

    private KafkaConsumer<String, String> createConsumer() {
        return new KafkaConsumer<>(
                ImmutableMap.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG, "tc-" + UUID.randomUUID(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
                ),
                new StringDeserializer(),
                new StringDeserializer()
        );
    }

    private KafkaProducer<String, String> createProducer() {
        return new KafkaProducer<>(
                ImmutableMap.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                        ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString()
                ),
                new StringSerializer(),
                new StringSerializer()
        );
    }

    private HikariDataSource createConnectionPool() {
        var config = new HikariConfig();
        config.setJdbcUrl(postgreSQL.getJdbcUrl());
        config.setUsername(postgreSQL.getUsername());
        config.setPassword(postgreSQL.getPassword());
        config.setDriverClassName(postgreSQL.getDriverClassName());
        return new HikariDataSource(config);
    }

    private void checkAndCreateRequiredTopics(AdminClient adminClient, List<NewTopic> topics) {
        try {
            Set<String> existingTopics = adminClient.listTopics().names().get();
            if (existingTopics.isEmpty()) {
                log.info("Topic not exist. Create topics {}", topics);
                adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);
            } else {
                topics.stream().map(NewTopic::name).filter(t -> !existingTopics.contains(t)).forEach(t -> {
                    try {
                        log.info("Topic not exist {}. Create topic {}", t, t);
                        adminClient.createTopics(List.of(new NewTopic(t, partitions, replicaFactor))).all().get(30, TimeUnit.SECONDS);
                    } catch (InterruptedException | TimeoutException | ExecutionException e) {
                        log.error("Error creating topic Kafka", e);
                    }
                });
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error checking topics", e);
        }
    }

    private void clearTable() {
        try {
            if (dataSource == null) {
                dataSource = createConnectionPool();
            }
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

            assertTrue(result.isEmpty());
        } catch (SQLException ex) {
            log.error("Error truncate table", ex);
        }
    }

    private void createAndCheckRuleInPostgreSQL(Long filterId, Long ruleId, String fieldName, String filterFunctionName, String filterValue) {
        try {
            if (dataSource == null) {
                dataSource = createConnectionPool();
            }
            log.info("Create filtering rule 1");
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

    private ConsumerRecords<String, String> getConsumerRecordsOutputTopic(KafkaConsumer<String, String> consumer, int retry, int timeoutSeconds) {
        boolean state = false;
        try {
            while (!state && retry > 0) {
                ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(100));
                if (consumerRecords.isEmpty()) {
                    log.info("Remaining attempts {}", retry);
                    retry--;
                    Thread.sleep(timeoutSeconds * 1000L);
                } else {
                    log.info("Read messages {}", consumerRecords.count());
                    return consumerRecords;
                }
            }
        } catch (InterruptedException ex) {
            log.error("Interrupt read messages", ex);
        }
        return ConsumerRecords.empty();
    }

    private Config replaceConfigForTest(Config config) {
        return config.withValue("kafka.consumer.bootstrap.servers", ConfigValueFactory.fromAnyRef(kafka.getBootstrapServers()))
                .withValue("kafka.producer.bootstrap.servers", ConfigValueFactory.fromAnyRef(kafka.getBootstrapServers()))
                .withValue("db.jdbcUrl", ConfigValueFactory.fromAnyRef(postgreSQL.getJdbcUrl()))
                .withValue("db.user", ConfigValueFactory.fromAnyRef(postgreSQL.getUsername()))
                .withValue("db.password", ConfigValueFactory.fromAnyRef(postgreSQL.getPassword()))
                .withValue("db.driver", ConfigValueFactory.fromAnyRef(postgreSQL.getDriverClassName()))
                .withValue("application.updateIntervalSec", ConfigValueFactory.fromAnyRef(10));
    }

    private Future<Boolean> testStartService(Config config) {
        return executorForTest.submit(() -> {
            serviceFiltering.start(config);
            return true;
        });
    }
}