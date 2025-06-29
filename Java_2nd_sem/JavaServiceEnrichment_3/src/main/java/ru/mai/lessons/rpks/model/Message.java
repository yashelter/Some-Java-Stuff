package ru.mai.lessons.rpks.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Message {
    private String value; // сообщение из Kafka в формате JSON
}
