package rpks;

import com.redis.testcontainers.RedisContainer;
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
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.JedisPooled;
import ru.mai.lessons.rpks.Service;
import ru.mai.lessons.rpks.impl.ServiceDeduplication;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
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

    @Container
    public RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:5.0.3-alpine"))
            .withExposedPorts(6379);

    ExecutorService executorForTest = Executors.newFixedThreadPool(2);

    private DataSource dataSource;
    private final String topicIn = "test_topic_in";
    private final String topicOut = "test_topic_out";
    private final short replicaFactor = 1;
    private final int partitions = 3;

    private final String tableName = "deduplication_rules";

    private final Service serviceDeduplication = new ServiceDeduplication();

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
     * Проверяет готовность Redis
     */
    @Test
    void testStartRedis() {
        assertTrue(redis.isRunning());
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

            var actualConsumerRecords = getConsumerRecordsOutputTopic(consumer, 1, 10, 1);
            actualConsumerRecords.forEach(actualRecords -> assertEquals(1, actualRecords.count()));
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Проверяет возможность читать, писать и удалять (expire) записи из Redis
     */
    @Test
    void testRedisReadWrite() {
        JedisPooled jedisPooled = createRedisClient();

        String testKey = "testKey";
        String expectedValue = "testValue";

        jedisPooled.set(testKey, expectedValue);

        String actualValue = jedisPooled.get(testKey);
        assertEquals(expectedValue, actualValue);

        jedisPooled.expire(testKey, 1);
        Awaitility.await()
                .atMost(3, TimeUnit.SECONDS)
                .until(() -> Optional.ofNullable(jedisPooled.get(testKey)).isEmpty());
    }


    /**
     * Тест проверяет возможноть чтения данных из PostgreSQL
     */
    @Test
    void testPostgreSQLReadValues() {
        clearTable();
        createAndCheckRuleInPostgreSQL(0L, 0L, "test_field", 10L, true);
    }

    /**
     * Тест проверяет следующее правило дедубликации: fieldName = 'name', timeToLiveSec = 5, isActive = true
     * Выполняется вставка правила в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - два из них подходят под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил дедубликации.
     * Ждём 15 секунд, пока Redis удалит ключи.
     * Снова отправляем два ожидаемых сообщения во входной топик, которые подходят под правила дедубликации.
     * Проверяем, что на выходе 4 сообщения с ожидаемым содержимым.
     */
    @Test
    void testServiceDeduplicationOneRule() {
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
            createAndCheckRuleInPostgreSQL(1L, 1L, "name", 10L, true);

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            String expectedJsonOne = "{\"name\":\"alex\", \"age\":18, \"sex\":\"M\"}";
            String expectedJsonTwo = "{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\"}";

            List<String> listExpectedJson = List.of(expectedJsonOne, expectedJsonTwo);

            listExpectedJson.forEach(json -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, "expected", json)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            Set.of("{\"name\":\"alex\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"alex\", \"age\":18, \"sex\":\"M\", \"money\":\"no\"}",
                    "{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\", \"money\":\"yes\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            log.info("Wait until Redis expired keys");
            Thread.sleep(15000L);

            listExpectedJson.forEach(json -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, "expected", json)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            var listConsumerRecords = getConsumerRecordsOutputTopic(consumer, 4, 30, 1);

            assertFalse(listConsumerRecords.isEmpty());
            var actualCountRecords = listConsumerRecords.stream().map(ConsumerRecords::count).reduce(0, Integer::sum);
            assertEquals(4, actualCountRecords);

            for (var consumerRecords : listConsumerRecords) {
                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    assertNotNull(consumerRecord.value());
                    assertTrue(listExpectedJson.contains(consumerRecord.value()));
                }
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
     * Тест проверяет следующее правила дедубликации: fieldName = 'name', timeToLiveSec = 5, isActive = true И
     * fieldName = 'age', timeToLiveSec = 10, isActive = true
     * Выполняется вставка правила в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - три из них подходят под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил дедубликации.
     * Ждём 5 секунд, но ключи не должны удалиться
     * Снова отправляем три сообщения во входной топик, которые уже не подходят под правила дедубликации.
     * Ждём 15 секунд, пока ключи действительно удалятся из Redis
     * Снова отправляем три сообщения во входной топик, которые подходят под правила дедубликации.
     * Проверяем, что на выходе 6 сообщения с ожидаемым содержимым.
     */
    @Test
    void testServiceDeduplicationTwoRules() {
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
            createAndCheckRuleInPostgreSQL(1L, 1L, "name", 10L, true);
            createAndCheckRuleInPostgreSQL(1L, 2L, "age", 20L, true);

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            String expectedJsonOne = "{\"name\":\"alex\", \"age\":18, \"sex\":\"M\"}";
            String expectedJsonTwo = "{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\"}";
            String expectedJsonThree = "{\"name\":\"alex\", \"age\":19, \"sex\":\"M\"}";

            List<String> listExpectedJson = List.of(expectedJsonOne, expectedJsonTwo, expectedJsonThree);

            listExpectedJson.forEach(json -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, "expected", json)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            Set.of("{\"name\":\"alex\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"alex\", \"age\":19, \"sex\":\"M\"}",
                    "{\"name\":\"alex\", \"age\":18, \"sex\":\"M\", \"money\":\"no\"}",
                    "{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\", \"money\":\"yes\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            log.info("Wait 5 seconds");
            Thread.sleep(5000L);

            listExpectedJson.forEach(json -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, "expected", json)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            log.info("Wait until Redis expired keys");
            Thread.sleep(15000L);

            listExpectedJson.forEach(json -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, "expected", json)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            var listConsumerRecords = getConsumerRecordsOutputTopic(consumer, 6, 30, 1);

            assertFalse(listConsumerRecords.isEmpty());
            var actualCountRecords = listConsumerRecords.stream().map(ConsumerRecords::count).reduce(0, Integer::sum);
            assertEquals(6, actualCountRecords);

            for (var consumerRecords : listConsumerRecords) {
                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    assertNotNull(consumerRecord.value());
                    assertTrue(listExpectedJson.contains(consumerRecord.value()));
                }
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
     * Тест проверяет следующее правила дедубликации: fieldName = 'name', timeToLiveSec = 5, isActive = true И
     * fieldName = 'age', timeToLiveSec = 10, isActive = false
     * Выполняется вставка правил в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - два из них подходят под правило.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил дедубликации.
     * Ждём 15 секунд, пока ключи удалятся из Redis
     * Снова отправляем три сообщения во входной топик, которые подходят под правила дедубликации.
     * Проверяем, что на выходе 4 сообщения с ожидаемым содержимым.
     */
    @Test
    void testServiceDeduplicationOneRuleIsActiveFalse() {
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
            createAndCheckRuleInPostgreSQL(1L, 1L, "name", 10L, true);
            createAndCheckRuleInPostgreSQL(1L, 2L, "age", 10L, false);

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            String expectedJsonOne = "{\"name\":\"alex\", \"age\":18, \"sex\":\"M\"}";
            String expectedJsonTwo = "{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\"}";
            String expectedJsonThree = "{\"name\":\"alex\", \"age\":19, \"sex\":\"M\"}";

            List<String> listExpectedJson = List.of(expectedJsonOne, expectedJsonTwo, expectedJsonThree);

            listExpectedJson.forEach(json -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, "expected", json)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            Set.of("{\"name\":\"alex\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\"}",
                    "{\"name\":\"alex\", \"age\":19, \"sex\":\"M\"}",
                    "{\"name\":\"alex\", \"age\":18, \"sex\":\"M\", \"money\":\"no\"}",
                    "{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\", \"money\":\"yes\"}",
                    "").forEach(negativeJson -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, negativeJson)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            log.info("Wait until Redis expired keys");
            Thread.sleep(15000L);

            listExpectedJson.forEach(json -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, "expected", json)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            var listConsumerRecords = getConsumerRecordsOutputTopic(consumer, 4, 30, 1);

            assertFalse(listConsumerRecords.isEmpty());
            var actualCountRecords = listConsumerRecords.stream().map(ConsumerRecords::count).reduce(0, Integer::sum);
            assertEquals(4, actualCountRecords);

            for (var consumerRecords : listConsumerRecords) {
                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    assertNotNull(consumerRecord.value());
                    assertTrue(listExpectedJson.contains(consumerRecord.value()));
                }
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
     * Тест проверяет следующее правила дедубликации: правил нет
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик.
     * Проверяется выходной топик - должен прочитать все сообщения.
     * Ждём 15 секунд
     * Снова отправляем три сообщения во входной топик.
     * Проверяем, что на выходе 6 сообщения с ожидаемым содержимым.
     */
    @Test
    void testServiceDeduplicationNoRules() {
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

            String expectedJsonOne = "{\"name\":\"alex\", \"age\":18, \"sex\":\"M\"}";
            String expectedJsonTwo = "{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\"}";
            String expectedJsonThree = "{\"name\":\"alex\", \"age\":19, \"sex\":\"M\"}";

            List<String> listExpectedJson = List.of(expectedJsonOne, expectedJsonTwo, expectedJsonThree);

            listExpectedJson.forEach(json -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, "expected", json)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            log.info("Wait 15 seconds");
            Thread.sleep(15000L);

            listExpectedJson.forEach(json -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, "expected", json)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            var listConsumerRecords = getConsumerRecordsOutputTopic(consumer, 6, 30, 1);

            assertFalse(listConsumerRecords.isEmpty());
            var actualCountRecords = listConsumerRecords.stream().map(ConsumerRecords::count).reduce(0, Integer::sum);
            assertEquals(6, actualCountRecords);

            for (var consumerRecords : listConsumerRecords) {
                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    assertNotNull(consumerRecord.value());
                    assertTrue(listExpectedJson.contains(consumerRecord.value()));
                }
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
     * Тест проверяет следующее правила дедубликации: fieldName = 'name', timeToLiveSec = 5, isActive = false И
     * fieldName = 'age', timeToLiveSec = 10, isActive = false
     * Выполняется вставка правила в базу PostgreSQL.
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик - ни одно из них не подходит под правила.
     * Проверяется выходной топик - должен прочитать сообщение, которое соответствует условиям правил дедубликации.
     * Ждём 10 секунд, но ключи не должны удалиться
     * Снова отправляем три сообщения во входной топик, которые уже не подходят под правила дедубликации.
     * Ждём 6 секунд, пока ключи действительно удалятся из Redis
     * Снова отправляем три сообщения во входной топик, которые подходят под правила дедубликации.
     * Проверяем, что на выходе 9 сообщения с ожидаемым содержимым.
     */
    @Test
    void testServiceDeduplicationNoActiveRules() {
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
            createAndCheckRuleInPostgreSQL(1L, 1L, "name", 5L, false);
            createAndCheckRuleInPostgreSQL(1L, 2L, "age", 10L, false);

            Config config = ConfigFactory.load();
            config = replaceConfigForTest(config);
            Future<Boolean> serviceIsWork = testStartService(config);

            String expectedJsonOne = "{\"name\":\"alex\", \"age\":18, \"sex\":\"M\"}";
            String expectedJsonTwo = "{\"name\":\"no_alex\", \"age\":18, \"sex\":\"M\"}";
            String expectedJsonThree = "{\"name\":\"alex\", \"age\":19, \"sex\":\"M\"}";

            List<String> listExpectedJson = List.of(expectedJsonOne, expectedJsonTwo, expectedJsonThree);

            listExpectedJson.forEach(json -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, "expected", json)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            log.info("Wait 10 seconds");
            Thread.sleep(10000L);

            listExpectedJson.forEach(json -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, "expected", json)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            log.info("Wait until Redis expired keys");
            Thread.sleep(10000L);

            listExpectedJson.forEach(json -> {
                try {
                    producer.send(new ProducerRecord<>(topicIn, "expected", json)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error send message to kafka topic", e);
                    fail();
                }
            });

            var listConsumerRecords = getConsumerRecordsOutputTopic(consumer, 9, 30, 1);

            assertFalse(listConsumerRecords.isEmpty());
            var actualCountRecords = listConsumerRecords.stream().map(ConsumerRecords::count).reduce(0, Integer::sum);
            assertEquals(9, actualCountRecords);

            for (var consumerRecords : listConsumerRecords) {
                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    assertNotNull(consumerRecord.value());
                    assertTrue(listExpectedJson.contains(consumerRecord.value()));
                }
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();
            clearTable();

        } catch (Exception e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    private JedisPooled createRedisClient() {
        return new JedisPooled(redis.getHost(), redis.getFirstMappedPort());
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
                            field("deduplication_id"),
                            field("rule_id"),
                            field("field_name"),
                            field("time_to_live_sec"),
                            field("is_active")
                    )
                    .from(table(tableName))
                    .fetch();

            assertTrue(result.isEmpty());
        } catch (SQLException ex) {
            log.error("Error truncate table", ex);
        }
    }

    private void createAndCheckRuleInPostgreSQL(Long deduplicationId, Long ruleId, String fieldName, Long timeToLiveSec, Boolean isActive) {
        try {
            if (dataSource == null) {
                dataSource = createConnectionPool();
            }
            log.info("Create filtering rule 1");
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

    private List<ConsumerRecords<String, String>> getConsumerRecordsOutputTopic(KafkaConsumer<String, String> consumer, int expectedCountMessages, int retry, int timeoutSeconds) {
        boolean state = false;
        int actualCountMessages = 0;
        List<ConsumerRecords<String, String>> actualRecords = new ArrayList<>();
        try {
            while (!state && retry > 0 && expectedCountMessages > actualCountMessages) {
                ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(100));
                if (consumerRecords.isEmpty()) {
                    log.info("Remaining attempts {}", retry);
                    retry--;
                    Thread.sleep(timeoutSeconds * 1000L);
                } else {
                    log.info("Read messages {}", consumerRecords.count());
                    actualCountMessages += consumerRecords.count();
                    actualRecords.add(consumerRecords);
                }
            }
        } catch (InterruptedException ex) {
            log.error("Interrupt read messages", ex);
        }
        return actualRecords;
    }

    private Config replaceConfigForTest(Config config) {
        return config.withValue("kafka.consumer.bootstrap.servers", ConfigValueFactory.fromAnyRef(kafka.getBootstrapServers()))
                .withValue("kafka.producer.bootstrap.servers", ConfigValueFactory.fromAnyRef(kafka.getBootstrapServers()))
                .withValue("db.jdbcUrl", ConfigValueFactory.fromAnyRef(postgreSQL.getJdbcUrl()))
                .withValue("db.user", ConfigValueFactory.fromAnyRef(postgreSQL.getUsername()))
                .withValue("db.password", ConfigValueFactory.fromAnyRef(postgreSQL.getPassword()))
                .withValue("db.driver", ConfigValueFactory.fromAnyRef(postgreSQL.getDriverClassName()))
                .withValue("application.updateIntervalSec", ConfigValueFactory.fromAnyRef(10))
                .withValue("redis.host", ConfigValueFactory.fromAnyRef(redis.getHost()))
                .withValue("redis.port", ConfigValueFactory.fromAnyRef(redis.getFirstMappedPort()));
    }

    private Future<Boolean> testStartService(Config config) throws InterruptedException {
        return testStartService(config, 10);
    }

    private Future<Boolean> testStartService(Config config, int startUpTimeoutSeconds) throws InterruptedException {
        var futureResult = executorForTest.submit(() -> {
            serviceDeduplication.start(config);
            return true;
        });
        log.info("Wait startup application {} seconds", startUpTimeoutSeconds);
        Thread.sleep(startUpTimeoutSeconds * 1000L);
        return futureResult;
    }
}