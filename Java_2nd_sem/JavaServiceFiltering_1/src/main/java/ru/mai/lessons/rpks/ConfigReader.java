package ru.mai.lessons.rpks;

import com.typesafe.config.Config;

public interface ConfigReader {
    public Config loadConfig(); // метод читает конфигурацию из файла *.conf
}
