package ru.mai.lessons.rpks;

import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.exception.FieldNotFoundInTableException;
import ru.mai.lessons.rpks.exception.WrongCommandFormatException;
import ru.mai.lessons.rpks.impl.DatabaseDriver;

import java.util.List;

@Slf4j
public class Main {
  public static void main(String[] args)
      throws FieldNotFoundInTableException, WrongCommandFormatException {
    log.info("Start service DatabaseDriver");
    IDatabaseDriver service = new DatabaseDriver(); // ваша реализация service
    String studentsCsvFile = args[0];
    String groupsCsvFile = args[1];
    String subjectsCsvFile = args[2];
    String gradeCsvFile = args[3];
    String command = args[4];
    List<String> results = service.find(studentsCsvFile, groupsCsvFile, subjectsCsvFile,
                                        gradeCsvFile, command);
    log.info("Found data: {}", results);
    log.info("Terminate service DatabaseDriver");
  }
}