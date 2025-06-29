package ru.mai.lessons.rpks;

import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.impl.ConfigurationReader;
import ru.mai.lessons.rpks.impl.ServiceFiltering;

@Slf4j
public class ServiceFilteringMain {
    public static void main(String[] args) {
        log.info("Start service Filtering");
        ConfigReader configReader = new ConfigurationReader();
        Service service = new ServiceFiltering(); // ваша реализация service
        service.start(configReader.loadConfig());
        log.info("Terminate service Filtering");
    }
}