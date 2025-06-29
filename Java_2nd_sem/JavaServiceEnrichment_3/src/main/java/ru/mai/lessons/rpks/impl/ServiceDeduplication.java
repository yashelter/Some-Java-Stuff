package ru.mai.lessons.rpks.impl;

import com.typesafe.config.Config;
import ru.mai.lessons.rpks.Service;

public class ServiceDeduplication implements Service {
    @Override
    public void start(Config config) {
        try (var client = KafkaFilterController.createNewInstance(config)) {
            client.processing();
        }
    }
}
