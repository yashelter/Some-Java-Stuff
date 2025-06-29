package rpks;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Sorts;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bson.Document;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;
import rpks.model.TestDataModel;
import ru.mai.lessons.rpks.Service;
import ru.mai.lessons.rpks.impl.ServiceEnrichment;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
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
    private final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"));

    ExecutorService executorForTest = Executors.newFixedThreadPool(2);

    private DataSource dataSource;
    private final static String TEST_TOPIC_IN = "test_topic_in";
    private final static String TEST_TOPIC_OUT = "test_topic_out";
    private final short replicaFactor = 1;
    private final int partitions = 3;

    private final String tableName = "enrichment_rules";

    private final Service serviceEnrichment = new ServiceEnrichment();

    private MongoClient mongoClient;

    private Consumer<String, String> consumer;
    private Producer<String, String> producer;
    private Admin adminClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> LIST_DEFAULT_DB = List.of("admin", "config", "local");

    private static final String MONGO_TEST_DB = "enrichment_db";
    private static final String MONGO_TEST_COLLECTION = "enrichment_collection";
    private static final String MONGO_TEST_CONDITION_FIELD_DOCUMENT = "condition_field_in_mongo";
    private static final String MONGO_TEST_DEFAULT_ENRICHMENT_VALUE = "default_value";

    private static final String MONGO_TEST_CONDITION_FIELD_VALUE = "condition_value";

    private static final Integer UPDATE_INTERVAL_POSTGRESQL_RULE_SECS = 10;

    private static final Long ENRICHMENT_ID = 1L;

    @BeforeEach
    void initClientsAndTestEnvironment() {
        dataSource = Optional.ofNullable(dataSource).orElse(createConnectionPool());
        adminClient = createAdminClient();
        consumer = createConsumer();
        producer = createProducer();
        List<NewTopic> topics = Stream.of(TEST_TOPIC_IN, TEST_TOPIC_OUT)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        checkAndCreateRequiredTopics(adminClient, topics);

        consumer.subscribe(Collections.singletonList(TEST_TOPIC_OUT));

        mongoClient = getMongoClient();
    }

    @AfterEach
    void clearDataAndCloseClients() {
        clearTable();
        List.of(producer, consumer, adminClient).forEach(client -> {
            Optional.ofNullable(client).ifPresent(c -> {
                try {
                    c.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        });

        Optional.ofNullable(mongoClient).ifPresent(mc -> {
            mc.listDatabaseNames().forEach(nameDb -> {
                if (!LIST_DEFAULT_DB.contains(nameDb)) {
                    mongoClient.getDatabase(nameDb).drop();
                }
            });
            mc.close();
        });
    }

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
     * Проверяет готовность MongoDB
     */
    @Test
    void testStartMongoDB() {
        assertTrue(mongoDBContainer.isRunning());
    }

    /**
     * Проверяет возможность читать и писать из Kafka
     */
    @Test
    void testKafkaWriteReadMessage() {
        try {
            log.info("Bootstrap.servers: {}", kafka.getBootstrapServers());
            log.info("Sending message");
            producer.send(new ProducerRecord<>(TEST_TOPIC_IN, "testKey", "json")).get(60, TimeUnit.SECONDS);
            log.info("Sent message");

            log.info("Consumer subscribe");
            consumer.subscribe(Collections.singletonList(TEST_TOPIC_IN));
            log.info("Consumer start reading");

            getConsumerRecordsOutputTopic(consumer, 10, 1);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет возможноть чтения данных из PostgreSQL
     */
    @Test
    void testPostgreSQLReadValues() {
        clearTable();
        createAndCheckRuleInPostgreSQL(0L, 0L, "test_field", "test_field_enrichment", "1", "0");
    }

    /**
     * Тест проверяет возможноть чтения и записи данных из MongoDB
     */
    @Test
    void testMongoReadWriteValues() {
        log.info("Connection mongodb {}", mongoDBContainer.getConnectionString());

        var mongoDatabase = mongoClient.getDatabase("test_db");
        log.info("Created mongo db test_db");

        mongoDatabase.createCollection("test_collection");
        log.info("Created mongo collection test_collection in test_db");

        log.info("Create document");
        var testDocument = new Document()
                .append("id", 1)
                .append("fieldName", "testFieldName")
                .append("fieldvValue", "testFieldValue")
                .append("fieldBoolean", true)
                .append("fieldArrays", Arrays.asList("test1", "test2", "test3"));
        log.info("Document created: {}", testDocument.toJson());

        log.info("Get collection");
        var testCollection = mongoDatabase.getCollection("test_collection");
        log.info("Collection: {}", testCollection);

        log.info("Insert document into collection");
        testCollection.insertOne(testDocument);

        var actualDocument = testCollection.find(eq("id", 1)).first();

        assertEquals(testDocument, actualDocument);
    }

    /**
     * Тест проверяет, что сервис обогащения без правил обогащения просто пропускает все сообщения в том виде, в котором они пришли<p>
     * Ход выполнения теста:<p>
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик.
     * Проверяется выходной топик.
     * Проверяем, что на выходе те же сообщения, что и на входе.
     */
    @Test
    void testServiceEnrichmentNoRules() {
        try {
            clearTable();

            Future<Boolean> serviceIsWork = testStartService();

            var listDataIn = List.of(
                    TestDataModel.builder().name("alex").age(18).sex("M").build(),
                    TestDataModel.builder().name("no_alex").age(19).sex("F").build()
            );

            listDataIn.forEach(data -> sendMessagesToTestTopic(producer, data));

            Thread.sleep(3000L);

            var consumerRecords = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1))
                    .get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(2, consumerRecords.count());

            var listExpectedJson = listDataIn.stream().map(this::toJson).toList();

            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertTrue(listExpectedJson.contains(consumerRecord.value()));
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет, что сервис обогащает сообщения по одному правилу:<p>
     * В поле enrichmentField вставляется документ из MongoDB, который удовлетворяет условию condition_field_in_mongo = condition_value<p>
     * Ход выполнения теста: <p>
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик.
     * Проверяется выходной топик.
     * Проверяем, что на выходе сообщения с обогащенными данными
     */
    @Test
    void testServiceEnrichmentOneRule() {
        try {
            createAndCheckRuleInPostgreSQL(
                    ENRICHMENT_ID,
                    1L,
                    "enrichmentField",
                    MONGO_TEST_CONDITION_FIELD_DOCUMENT,
                    MONGO_TEST_CONDITION_FIELD_VALUE,
                    MONGO_TEST_DEFAULT_ENRICHMENT_VALUE);

            Document testDocument = new Document()
                    .append("testFieldString", "testString")
                    .append("testFieldNumeric", 1)
                    .append("testFieldArray", List.of(1))
                    .append("testFieldObject", new Document().append("testInnerFieldString", "testInnerString"))
                    .append(MONGO_TEST_CONDITION_FIELD_DOCUMENT, MONGO_TEST_CONDITION_FIELD_VALUE);

            createAndCheckDocumentInMongoDB(testDocument);

            var serviceIsWork = testStartService();

            var listDataIn = List.of(
                    TestDataModel.builder().name("alex").age(18).sex("M").build(),
                    TestDataModel.builder().name("no_alex").age(19).sex("F").build()
            );

            listDataIn.forEach(data -> sendMessagesToTestTopic(producer, data));

            Thread.sleep(3000L);

            var consumerRecords = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1))
                    .get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(2, consumerRecords.count());

            var listExpectedJson = listDataIn.stream().map(data -> {
                data.setEnrichmentField(testDocument.toJson());
                return toJsonNode(toJson(data));
            }).toList();

            for (var consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertTrue(listExpectedJson.contains(toJsonNode(consumerRecord.value())));
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();

        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет, что сервис обогащает сообщения по одному правилу:<p>
     * В поле enrichmentField вставляется значение по умолчанию, так как в MongoDB нет документа который удовлетворяет условию condition_field_in_mongo = condition_value
     * <p>
     * Ход выполнения теста:<p>
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик.
     * Проверяется выходной топик.
     * Проверяем, что на выходе сообщения с обогащенными данными
     */
    @Test
    void testServiceEnrichmentDefaultValue() {
        try {
            createAndCheckRuleInPostgreSQL(
                    ENRICHMENT_ID,
                    1L,
                    "enrichmentField",
                    MONGO_TEST_CONDITION_FIELD_DOCUMENT,
                    MONGO_TEST_CONDITION_FIELD_VALUE + "_not_expected",
                    MONGO_TEST_DEFAULT_ENRICHMENT_VALUE);

            Document testDocument = new Document()
                    .append("testFieldString", "testString")
                    .append("testFieldNumeric", 1)
                    .append("testFieldArray", List.of(1))
                    .append("testFieldObject", new Document().append("testInnerFieldString", "testInnerString"))
                    .append(MONGO_TEST_CONDITION_FIELD_DOCUMENT, MONGO_TEST_CONDITION_FIELD_VALUE);

            createAndCheckDocumentInMongoDB(testDocument);

            var serviceIsWork = testStartService();

            var listDataIn = List.of(
                    TestDataModel.builder().name("alex").age(18).sex("M").build(),
                    TestDataModel.builder().name("no_alex").age(19).sex("F").build()
            );

            listDataIn.forEach(data -> sendMessagesToTestTopic(producer, data));

            Thread.sleep(3000L);

            var consumerRecords = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1))
                    .get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(2, consumerRecords.count());

            var listExpectedJson = listDataIn.stream().map(data -> {
                data.setEnrichmentField("\"" + MONGO_TEST_DEFAULT_ENRICHMENT_VALUE + "\"");
                return toJsonNode(toJson(data));
            }).toList();

            for (var consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertTrue(listExpectedJson.contains(toJsonNode(consumerRecord.value())));
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();

        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет, что сервис обогащает сообщения по одному правилу:
     * <p>
     * В поле enrichmentField вставляется документ, который удовлетворяет условию condition_field_in_mongo = condition_value,
     * но так как в базе два таких документа, то берется последний как самый актуальный. Самый актуальный, у которого id больше остальных.
     * <p>
     * Ход выполнения теста:
     * <p>
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик.
     * Проверяется выходной топик.
     * Проверяем, что на выходе сообщения с обогащенными данными
     */
    @Test
    void testServiceEnrichmentActualDocument() {
        try {
            createAndCheckRuleInPostgreSQL(
                    ENRICHMENT_ID,
                    1L,
                    "enrichmentField",
                    MONGO_TEST_CONDITION_FIELD_DOCUMENT,
                    MONGO_TEST_CONDITION_FIELD_VALUE,
                    MONGO_TEST_DEFAULT_ENRICHMENT_VALUE);

            Document testDocumentOne = new Document()
                    .append("testFieldString", "testString1")
                    .append("testFieldNumeric", 1)
                    .append("testFieldArray", List.of(1))
                    .append("testFieldObject", new Document().append("testInnerFieldString", "testInnerString1"))
                    .append(MONGO_TEST_CONDITION_FIELD_DOCUMENT, MONGO_TEST_CONDITION_FIELD_VALUE);
            createAndCheckDocumentInMongoDB(testDocumentOne);

            Document testDocumentTwo = new Document()
                    .append("testFieldString", "testString2")
                    .append("testFieldNumeric", 2)
                    .append("testFieldArray", List.of(2))
                    .append("testFieldObject", new Document().append("testInnerFieldString", "testInnerString2"))
                    .append(MONGO_TEST_CONDITION_FIELD_DOCUMENT, MONGO_TEST_CONDITION_FIELD_VALUE);
            createAndCheckDocumentInMongoDB(testDocumentTwo);

            var serviceIsWork = testStartService();

            var listDataIn = List.of(
                    TestDataModel.builder().name("alex").age(18).sex("M").build(),
                    TestDataModel.builder().name("no_alex").age(19).sex("F").build()
            );

            listDataIn.forEach(data -> sendMessagesToTestTopic(producer, data));

            Thread.sleep(3000L);

            var consumerRecords = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1))
                    .get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(2, consumerRecords.count());

            var listExpectedJson = listDataIn.stream().map(data -> {
                data.setEnrichmentField(testDocumentTwo.toJson());
                return toJsonNode(toJson(data));
            }).toList();

            for (var consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertTrue(listExpectedJson.contains(toJsonNode(consumerRecord.value())));
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();

        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет, что сервис обогащает сообщения по двум правилам:<p>
     * 1. В поле enrichmentField вставляется документ из MongoDB, который удовлетворяет условию condition_field_in_mongo = condition_value<p>
     * 2. В поле enrichmentOtherField вставляется документ из MongoDB, который удовлетворяет условию condition_field_in_mongo = condition_value_other<p>
     * Ход выполнения теста: <p>
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик.
     * Проверяется выходной топик.
     * Проверяем, что на выходе сообщения с обогащенными данными
     */
    @Test
    void testServiceEnrichmentTwoRules() {
        try {
            createAndCheckRuleInPostgreSQL(
                    ENRICHMENT_ID,
                    1L,
                    "enrichmentField",
                    MONGO_TEST_CONDITION_FIELD_DOCUMENT,
                    MONGO_TEST_CONDITION_FIELD_VALUE,
                    MONGO_TEST_DEFAULT_ENRICHMENT_VALUE);

            createAndCheckRuleInPostgreSQL(
                    ENRICHMENT_ID,
                    2L,
                    "enrichmentOtherField",
                    MONGO_TEST_CONDITION_FIELD_DOCUMENT,
                    MONGO_TEST_CONDITION_FIELD_VALUE + "_other",
                    MONGO_TEST_DEFAULT_ENRICHMENT_VALUE);

            Document testDocumentOne = new Document()
                    .append("testFieldString", "testString")
                    .append("testFieldNumeric", 1)
                    .append("testFieldArray", List.of(1))
                    .append("testFieldObject", new Document().append("testInnerFieldString", "testInnerString"))
                    .append(MONGO_TEST_CONDITION_FIELD_DOCUMENT, MONGO_TEST_CONDITION_FIELD_VALUE);

            createAndCheckDocumentInMongoDB(testDocumentOne);

            Document testDocumentTwo = new Document()
                    .append("testFieldName", "testName")
                    .append(MONGO_TEST_CONDITION_FIELD_DOCUMENT, MONGO_TEST_CONDITION_FIELD_VALUE + "_other");

            createAndCheckDocumentInMongoDB(testDocumentTwo, MONGO_TEST_CONDITION_FIELD_DOCUMENT, MONGO_TEST_CONDITION_FIELD_VALUE + "_other");

            var serviceIsWork = testStartService();

            var listDataIn = List.of(
                    TestDataModel.builder().name("alex").age(18).sex("M").build(),
                    TestDataModel.builder().name("no_alex").age(19).sex("F").build()
            );

            listDataIn.forEach(data -> sendMessagesToTestTopic(producer, data));

            Thread.sleep(3000L);

            var consumerRecords = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1))
                    .get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(2, consumerRecords.count());

            var listExpectedJson = listDataIn.stream().map(data -> {
                data.setEnrichmentField(testDocumentOne.toJson());
                data.setEnrichmentOtherField(testDocumentTwo.toJson());
                return toJsonNode(toJson(data));
            }).toList();

            for (var consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertTrue(listExpectedJson.contains(toJsonNode(consumerRecord.value())));
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();

        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет, что сервис обогащает сообщения только по одному правилу, если в PostgreSQL два разных правила, но обогащают одно и то же поле:<p>
     * 1. В поле enrichmentField вставляется документ из MongoDB, который удовлетворяет условию condition_field_in_mongo = condition_value<p>
     * 2. В поле enrichmentField вставляется документ из MongoDB, который удовлетворяет условию condition_field_in_mongo = condition_value_other<p>
     * Должно сработать то правило, чей rule_id больше - оно считается более актуальным.<p>
     * Ход выполнения теста: <p>
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик.
     * Проверяется выходной топик.
     * Проверяем, что на выходе сообщения с обогащенными данными
     */
    @Test
    void testServiceEnrichmentActualRule() {
        try {
            createAndCheckRuleInPostgreSQL(
                    ENRICHMENT_ID,
                    1L,
                    "enrichmentField",
                    MONGO_TEST_CONDITION_FIELD_DOCUMENT,
                    MONGO_TEST_CONDITION_FIELD_VALUE + "_other",
                    MONGO_TEST_DEFAULT_ENRICHMENT_VALUE);

            createAndCheckRuleInPostgreSQL(
                    ENRICHMENT_ID,
                    2L,
                    "enrichmentField",
                    MONGO_TEST_CONDITION_FIELD_DOCUMENT,
                    MONGO_TEST_CONDITION_FIELD_VALUE,
                    MONGO_TEST_DEFAULT_ENRICHMENT_VALUE);

            Document testDocumentOne = new Document()
                    .append("testFieldName", "testName")
                    .append(MONGO_TEST_CONDITION_FIELD_DOCUMENT, MONGO_TEST_CONDITION_FIELD_VALUE + "_other");

            createAndCheckDocumentInMongoDB(testDocumentOne, MONGO_TEST_CONDITION_FIELD_DOCUMENT, MONGO_TEST_CONDITION_FIELD_VALUE + "_other");

            Document testDocumentTwo = new Document()
                    .append("testFieldString", "testString")
                    .append("testFieldNumeric", 1)
                    .append("testFieldArray", List.of(1))
                    .append("testFieldObject", new Document().append("testInnerFieldString", "testInnerString"))
                    .append(MONGO_TEST_CONDITION_FIELD_DOCUMENT, MONGO_TEST_CONDITION_FIELD_VALUE);

            createAndCheckDocumentInMongoDB(testDocumentTwo);

            var serviceIsWork = testStartService();

            var listDataIn = List.of(
                    TestDataModel.builder().name("alex").age(18).sex("M").build(),
                    TestDataModel.builder().name("no_alex").age(19).sex("F").build()
            );

            listDataIn.forEach(data -> sendMessagesToTestTopic(producer, data));

            Thread.sleep(3000L);

            var consumerRecords = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1))
                    .get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(2, consumerRecords.count());

            var listExpectedJson = listDataIn.stream().map(data -> {
                data.setEnrichmentField(testDocumentTwo.toJson());
                return toJsonNode(toJson(data));
            }).toList();

            for (var consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertTrue(listExpectedJson.contains(toJsonNode(consumerRecord.value())));
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();

        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    /**
     * Тест проверяет, что сервис обогащает сообщения и проверяет обновление правил в PostgreSQL:<p>
     * 1. В поле enrichmentField вставляется документ из MongoDB, который удовлетворяет условию condition_field_in_mongo = condition_value<p>
     * 2. Обновленное правило - в поле enrichmentField вставляется документ из MongoDB, который удовлетворяет условию condition_field_in_mongo = condition_value_other<p>
     * Ход выполнения теста: <p>
     * Запускается приложение с тестовыми конфигурациями в test/resources/application.conf.
     * Отправляется несколько сообщений во входной топик.
     * Ждем интервал времени на обработку сообщений
     * Обновляется правило.
     * Ждем обновления правила в приложении
     * Снова отправляется несколько сообщений во входной топик.
     * Проверяем, что на выходе сообщения с обогащенными данными
     */
    @Test
    void testServiceEnrichmentUpdateRule() {
        try {
            createAndCheckRuleInPostgreSQL(
                    ENRICHMENT_ID,
                    1L,
                    "enrichmentField",
                    MONGO_TEST_CONDITION_FIELD_DOCUMENT,
                    MONGO_TEST_CONDITION_FIELD_VALUE,
                    MONGO_TEST_DEFAULT_ENRICHMENT_VALUE);

            Document testDocumentOne = new Document()
                    .append("testFieldName", "testName")
                    .append(MONGO_TEST_CONDITION_FIELD_DOCUMENT, MONGO_TEST_CONDITION_FIELD_VALUE + "_other");

            createAndCheckDocumentInMongoDB(testDocumentOne, MONGO_TEST_CONDITION_FIELD_DOCUMENT, MONGO_TEST_CONDITION_FIELD_VALUE + "_other");

            Document testDocumentTwo = new Document()
                    .append("testFieldString", "testString")
                    .append("testFieldNumeric", 1)
                    .append("testFieldArray", List.of(1))
                    .append("testFieldObject", new Document().append("testInnerFieldString", "testInnerString"))
                    .append(MONGO_TEST_CONDITION_FIELD_DOCUMENT, MONGO_TEST_CONDITION_FIELD_VALUE);

            createAndCheckDocumentInMongoDB(testDocumentTwo);

            var serviceIsWork = testStartService();

            var listDataIn = List.of(
                    TestDataModel.builder().name("alex").age(18).sex("M").build(),
                    TestDataModel.builder().name("no_alex").age(19).sex("F").build()
            );

            listDataIn.forEach(data -> sendMessagesToTestTopic(producer, data));

            Thread.sleep(3000L);

            var listExpectedJson = listDataIn.stream().map(data -> {
                data.setEnrichmentField(testDocumentTwo.toJson());
                return toJsonNode(toJson(data));
            }).toList();

            log.info("Wait until messages processed");
            Thread.sleep(UPDATE_INTERVAL_POSTGRESQL_RULE_SECS * 1000L);

            clearTable();
            createAndCheckRuleInPostgreSQL(
                    ENRICHMENT_ID,
                    2L,
                    "enrichmentOtherField",
                    MONGO_TEST_CONDITION_FIELD_DOCUMENT,
                    MONGO_TEST_CONDITION_FIELD_VALUE + "_other",
                    MONGO_TEST_DEFAULT_ENRICHMENT_VALUE);

            log.info("Wait until rules updated");
            Thread.sleep((UPDATE_INTERVAL_POSTGRESQL_RULE_SECS + 1) * 1000L);

            var listDataInAfterUpdateRule = List.of(
                    TestDataModel.builder().name("alex").age(18).sex("M").build(),
                    TestDataModel.builder().name("no_alex").age(19).sex("F").build()
            );

            listDataInAfterUpdateRule.forEach(data -> sendMessagesToTestTopic(producer, data));

            var consumerRecords = executorForTest.submit(() -> getConsumerRecordsOutputTopic(consumer, 10, 1))
                    .get(60, TimeUnit.SECONDS);

            assertFalse(consumerRecords.isEmpty());
            assertEquals(4, consumerRecords.count());

            var listExpectedJsonAfterUpdated = listDataInAfterUpdateRule.stream().map(data -> {
                data.setEnrichmentOtherField(testDocumentOne.toJson());
                return toJsonNode(toJson(data));
            }).toList();

            for (var consumerRecord : consumerRecords) {
                assertNotNull(consumerRecord.value());
                assertTrue(listExpectedJson.contains(toJsonNode(consumerRecord.value())) || listExpectedJsonAfterUpdated.contains(toJsonNode(consumerRecord.value())));
            }

            serviceIsWork.cancel(true);
            executorForTest.shutdown();

        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            log.error("Error test execution", e);
            fail();
        }
    }

    private AdminClient createAdminClient() {
        log.info("Create admin client");
        return AdminClient.create(ImmutableMap.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()));
    }

    private KafkaConsumer<String, String> createConsumer() {
        log.info("Create consumer");
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
        log.info("Create producer");
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
        log.info("Cretae connection pool");
        var config = new HikariConfig();
        config.setJdbcUrl(postgreSQL.getJdbcUrl());
        config.setUsername(postgreSQL.getUsername());
        config.setPassword(postgreSQL.getPassword());
        config.setDriverClassName(postgreSQL.getDriverClassName());
        return new HikariDataSource(config);
    }

    private void checkAndCreateRequiredTopics(Admin adminClient, List<NewTopic> topics) {
        log.info("Check required topics");
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
        log.info("Clear table PostgreSQL");
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

            assertTrue(result.isEmpty());
        } catch (SQLException ex) {
            log.error("Error truncate table", ex);
        }
    }

    private void createAndCheckRuleInPostgreSQL(Long enrichmentId, Long ruleId, String fieldName, String fieldNameEnrichment, String fieldValue, String fieldValueDefault) {
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

    private ConsumerRecords<String, String> getConsumerRecordsOutputTopic(Consumer<String, String> consumer, int retry, int timeoutSeconds) {
        log.info("Start reading messages from kafka");
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
        log.info("Replace test config by container values");
        return config.withValue("kafka.consumer.bootstrap.servers", ConfigValueFactory.fromAnyRef(kafka.getBootstrapServers()))
                .withValue("kafka.producer.bootstrap.servers", ConfigValueFactory.fromAnyRef(kafka.getBootstrapServers()))
                .withValue("db.jdbcUrl", ConfigValueFactory.fromAnyRef(postgreSQL.getJdbcUrl()))
                .withValue("db.user", ConfigValueFactory.fromAnyRef(postgreSQL.getUsername()))
                .withValue("db.password", ConfigValueFactory.fromAnyRef(postgreSQL.getPassword()))
                .withValue("db.driver", ConfigValueFactory.fromAnyRef(postgreSQL.getDriverClassName()))
                .withValue("application.updateIntervalSec", ConfigValueFactory.fromAnyRef(UPDATE_INTERVAL_POSTGRESQL_RULE_SECS))
                .withValue("application.enrichmentId", ConfigValueFactory.fromAnyRef(ENRICHMENT_ID))
                .withValue("mongo.connectionString", ConfigValueFactory.fromAnyRef(mongoDBContainer.getConnectionString()))
                .withValue("mongo.database", ConfigValueFactory.fromAnyRef(MONGO_TEST_DB))
                .withValue("mongo.collection", ConfigValueFactory.fromAnyRef(MONGO_TEST_COLLECTION));
    }

    private Future<Boolean> testStartService() {
        log.info("Start test service enrichment");
        Config config = ConfigFactory.load();
        return executorForTest.submit(() -> {
            serviceEnrichment.start(replaceConfigForTest(config));
            return true;
        });
    }

    private MongoClient getMongoClient() {
        log.info("Create mongo client");
        return Optional.ofNullable(mongoClient).orElse(MongoClients.create(mongoDBContainer.getConnectionString()));
    }

    private void createAndCheckDocumentInMongoDB(Document document) {
        createAndCheckDocumentInMongoDB(document, MONGO_TEST_CONDITION_FIELD_DOCUMENT, MONGO_TEST_CONDITION_FIELD_VALUE);
    }

    private void createAndCheckDocumentInMongoDB(Document document, String conditionField, String conditionValue) {
        log.info("Create and check document in MongoDB by condition {} = {}", conditionField, conditionValue);
        var mongoCollection = mongoClient.getDatabase(MONGO_TEST_DB).getCollection(MONGO_TEST_COLLECTION);
        mongoCollection.insertOne(document);
        var actualDocument = Optional.ofNullable(mongoCollection
                .find(eq(conditionField, conditionValue))
                .sort(Sorts.descending("_id"))
                .first());
        assertFalse(actualDocument.isEmpty());
        assertEquals(document, actualDocument.get());

        log.info("Documents in MongoDB:");
        mongoCollection
                .find(eq(conditionField, conditionValue))
                .forEach(d -> log.info("Document: {}", d));

    }

    private String toJson(Object object) {
        String json = "{}";
        try {
            json = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error convert object to json", e);
        }
        return json;
    }

    private void sendMessagesToTestTopic(Producer<String, String> producer, Object data) {
        log.info("Send message to kafka {}", data);
        try {
            producer.send(new ProducerRecord<>(TEST_TOPIC_IN, "expected", toJson(data))).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error send message to kafka topic", e);
            fail();
        }
    }

    private JsonNode toJsonNode(String json) {
        JsonNode jsonNode = objectMapper.createObjectNode();
        try {
            jsonNode = objectMapper.readTree(json);
        } catch (IOException e) {
            log.error("Error transformation json string to json node {}", json);
            fail();
        }
        return jsonNode;
    }
}