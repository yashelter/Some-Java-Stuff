package ru.mai.lessons.rpks.model;

import lombok.Getter;

@Getter
public class Rule {
    private Long deduplicationId; // id сервиса дедубликации
    private Long ruleId; // id правила дедубликации
    private String fieldName; // ключ - поле сообщения, по которому выполняем дедубликацию { "name": "Jhonas"}, fieldName = "name", значит все сообщения со значением "Jhonas" в поле "name" будут считаться дублями если пришли в указанный промежуток времени timeToLiveSec
    private Long timeToLiveSec; // время жизни ключа в Redis
    private Boolean isActive; // если true, то это правило дедубликации активно и его нужно применять, если false, то правило не применяется.
}
