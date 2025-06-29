package ru.mai.lessons.rpks.model;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@Getter
public class Message {
    private String value; // сообщение из Kafka в формате JSON

    private boolean filterState; // true - удовлетворены условиях всех правил (Rule), false - хотя бы одно условие не прошло проверку.

    public Message(Message other) {
        value = other.value;
        filterState = other.filterState;
    }
}
