package ru.mai.lessons.rpks.impl;

import ru.mai.lessons.rpks.IDatabaseDriver;

import java.util.List;

public class DatabaseDriver implements IDatabaseDriver {
  @Override
  public List<String> find(String studentsCsvFile, String groupsCsvFile, String subjectsCsvFile,
                           String gradeCsvFile, String command) {
    return null; // реализовать проверку
  }
}
