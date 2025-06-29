package ru.mai.lessons.rpks.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@UtilityClass
public class JsonUtils {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public String toJson(Object object) {
    String json = "{}";

    try {
      json = OBJECT_MAPPER.writeValueAsString(object);
    } catch (JsonProcessingException ex) {
      log.error("Произошла ошибка во время преобразования объекта в JSON строку");
    }

    return json;
  }
}
