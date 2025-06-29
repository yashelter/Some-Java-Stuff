package ru.mai.lessons.rpks;

import ru.mai.lessons.rpks.model.Message;
import ru.mai.lessons.rpks.model.Rule;

public interface RuleProcessor {
    public Message processing(Message message, Rule[] rules); // применяет правила фильтрации к сообщениям и устанавливает в них filterState значение true, если сообщение удовлетворяет условиям всех правил.
}
