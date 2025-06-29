package ru.mai.lessons.rpks;

import ru.mai.lessons.rpks.model.Message;
import ru.mai.lessons.rpks.model.Rule;

public interface RuleProcessor {
    public Message processing(Message message, Rule[] rules); // применяет правила дедубликации к сообщениям и устанавливает в них deduplicationState значение true, если сообщение удовлетворяет условиям всех правил. Несколько правил объединяются в один ключ, значит если несколько правил, то из них составляет один ключ и одним запросом проверяется в Redis. Если у правил разное время, то берётся большее из них.
}
