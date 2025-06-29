package ru.mai.lessons.rpks.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ru.mai.lessons.rpks.ConfigReader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigurationReader implements ConfigReader {
    @Override
    public Config loadConfig() {
        log.info("Load configuration");
        return ConfigFactory.load();
    }
}
