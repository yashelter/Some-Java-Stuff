package ru.mai.lessons.rpks.impl;

import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import ru.mai.lessons.rpks.DbReader;
import ru.mai.lessons.rpks.KafkaReader;
import ru.mai.lessons.rpks.KafkaWriter;
import ru.mai.lessons.rpks.RuleProcessor;
import ru.mai.lessons.rpks.model.Message;
import ru.mai.lessons.rpks.model.Rule;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Slf4j
public final class KafkaFilterController implements KafkaReader, AutoCloseable {
    private static final String KAFKA_CONSUMER_PREFIX = "kafka.consumer.";
    private final DbReader dbReader;
    private final KafkaWriter writer;
    private final RuleProcessor ruleProcessor;
    private final Duration timeoutTime;
    private final KafkaConsumer<String, String> consumer;

    public static KafkaFilterController createNewInstance(Config config) {
        DbReader reader = new DbReaderController(config);
        KafkaWriter writer = new KafkaWriterController(config);
        RuleProcessor processor = new RulesController(config);

        return new KafkaFilterController(config, reader, writer, processor, Duration.ofMillis(52));
    }

    public KafkaFilterController(Config config, DbReader dbReader, KafkaWriter writer,
                                 RuleProcessor ruleProcessor, Duration timeoutTime) {
        this.dbReader = dbReader;
        this.writer = writer;
        this.ruleProcessor = ruleProcessor;
        this.timeoutTime = timeoutTime;

        log.info("Initializing KafkaConsumer properties");

        Properties properties = new Properties();
        log.info(config.getString(KAFKA_CONSUMER_PREFIX + "bootstrap.servers"));
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                config.getString(KAFKA_CONSUMER_PREFIX + "bootstrap.servers"));
        properties.put(ConsumerConfig.GROUP_ID_CONFIG,
                config.getString(KAFKA_CONSUMER_PREFIX + "group.id"));
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                config.getString(KAFKA_CONSUMER_PREFIX + "auto.offset.reset"));
        String inputTopic = config.getString(KAFKA_CONSUMER_PREFIX + "inputTopic");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        log.info("Creating KafkaConsumer");
        consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singletonList(inputTopic));
    }


    @Override
    public void processing() {
        try {
            log.info("Starting processing");
            for(;;) {
                log.debug("Getting new messages...");
                ConsumerRecords<String, String> records = consumer.poll(timeoutTime);
                if (records.isEmpty()) {
                    continue;
                }
                log.info("Received {} messages.", records.count());
                Rule[] rules = dbReader.readRulesFromDB();

                for (ConsumerRecord<String, String> recording : records) {
                    Message message = ruleProcessor.processing(
                            new Message(recording.value()), rules);
                    log.error("Processed message {}", message.toString());
                    writer.processing(message);
                }
                commitChanges();
            }
        } catch (WakeupException e) {
            log.info("Kafka consumer polling loop woken up for shutdown.");
        } catch (Exception e) {
            log.error("Error during Kafka consumption loop", e);
        } finally {
            closeResources();
            log.info("Kafka processor resources closed.");
        }
    }


    private void commitChanges() {
        try {
            consumer.commitSync();
            log.debug("Offsets committed successfully.");
        } catch (Exception e) {
            log.error("Failed to commit offsets", e);
        }
    }


    private void closeResources() {
        log.debug("Closing Kafka consumer, writer, and DB reader...");
        try {
            consumer.close();
        } catch (Exception e) {
            log.error("Error closing Kafka consumer", e);
        }
        try {
            writer.close();
        } catch (Exception e) {
            log.error("Error closing writer", e);
        }
        try {
            dbReader.close();
        } catch (Exception e) {
            log.error("Error closing DB reader", e);
        }
        log.info("Finished closing resources.");
    }

    @Override
    public void close() {
        closeResources();
    }
}
