package ru.mai.lessons.rpks;

public interface RedisClient {
    /** Нужно реализовать этот интерфейс таким образом:
     Чтение данных по ключу, чтобы проверить есть ли уже такой ключ в Redis.
     Если есть, значит это дубликат и устанавливаем deduplicationState = false.
     Если нет, значит вставляем это значение в Redis, устанавливаем время жизни сообщения по правилу из PostgreSQL и проставляем deduplicationState = true.
     Реализация RedisClient должна работать в RuleProcessor.
    */
    public boolean tryInsertInDB(String key, Long lifeTime);
    public void close();
}
