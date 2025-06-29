package ru.mai.lessons.rpks.impl;

import com.typesafe.config.Config;
import ru.mai.lessons.rpks.Service;

public class ServiceFiltering implements Service {
    @Override
    public void start(Config config) {
        try (var controller = KafkaFilterController.createNewInstance(config)){
            controller.processing();
        }

    }
}
