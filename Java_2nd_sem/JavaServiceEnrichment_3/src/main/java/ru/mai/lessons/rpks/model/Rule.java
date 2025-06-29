package ru.mai.lessons.rpks.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Rule {
    private long id; // global id
    private Long enricherId; // id обогатителя
    private Long ruleId; // id правила обогащения
    private String fieldName; // поле сообщения, которое нужно обогатить
    private String fieldNameEnrichment; // название поля в коллекции MongoDB для обогащения
    private String fieldValue; // из какого поля сообщения нужно брать значение поля fieldNameEnrichment, по которому нужно найти документ в коллекции MongoDB
    private String fieldValueDefault; // значение по умолчанию, если значение для обогащения не найдено в MongoDB
}
