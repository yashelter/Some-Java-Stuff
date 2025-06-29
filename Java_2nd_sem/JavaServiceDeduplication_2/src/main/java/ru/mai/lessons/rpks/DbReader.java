package ru.mai.lessons.rpks;

import ru.mai.lessons.rpks.model.Rule;


public interface DbReader {
    public Rule[] readRulesFromDB(); // метод получает набор правил из БД PostgreSQL. Конфигурация для подключения из файла *.conf. Метод также должен проверять в заданное время с периодом изменения в БД и обновлять правила.
    public void close();
}
