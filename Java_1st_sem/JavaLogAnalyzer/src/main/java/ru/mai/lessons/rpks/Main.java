package ru.mai.lessons.rpks;

import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.exception.WrongFilenameException;
import ru.mai.lessons.rpks.impl.LogAnalyzer;

import java.util.List;

@Slf4j
public class Main {
  public static void main(String[] args) throws WrongFilenameException {
    log.info("Start service LogAnalyzer");
    ILogAnalyzer service = new LogAnalyzer(); // ваша реализация service
    List<Integer> errorQueryIds = service.analyze(args[0], args[1]);
    log.info("Found error queries: {}", errorQueryIds);
    log.info("Terminate service LogAnalyzer");
  }
}