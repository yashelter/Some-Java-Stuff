package ru.mai.lessons.rpks;

import ru.mai.lessons.rpks.model.Message;

public interface KafkaWriter {
    public void processing(Message message); // отправляет сообщения в выходной топик. Конфигурация берется из файла *.conf
    public void close();
}
