package ru.mai.lessons.rpks.impl;

import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import ru.mai.lessons.rpks.KafkaWriter;
import ru.mai.lessons.rpks.model.Message;

import java.util.Properties;

@Slf4j
public class KafkaWriterController implements KafkaWriter {
    private static final String KAFKA_PRODUCER_PREFIX = "kafka.producer.";
    private final String topic;
    private final KafkaProducer<String, String> producer;


    public KafkaWriterController(Config config) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", config.getString(KAFKA_PRODUCER_PREFIX + "bootstrap.servers"));
        properties.put("key.serializer", StringSerializer.class.getName());
        properties.put("value.serializer", StringSerializer.class.getName());

        topic = config.getString(KAFKA_PRODUCER_PREFIX + "topic");
        producer = new KafkaProducer<>(properties);
    }


    @Override
    public void processing(Message message) {
        if (message.isDeduplicationState()){
            log.debug("Processing message: {}", message);
            producer.send(new ProducerRecord<>(topic, message.getValue()));
        }
    }

    @Override
    public void close() {
        producer.flush();
        producer.close();
    }
}
