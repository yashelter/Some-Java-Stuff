package ru.mai.lessons.rpks;

import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.exception.DirectoryAccessException;
import ru.mai.lessons.rpks.impl.DirectorySizeChecker;

@Slf4j
public class Main {
  public static void main(String[] args) throws DirectoryAccessException {
    log.info("Start service DirectorySizeChecker");
    String directoryName = args[0];
    IDirectorySizeChecker service = new DirectorySizeChecker(); // ваша реализация service
    String directorySize = service.checkSize(directoryName);
    log.info("{} ---- {}", directoryName, directorySize);
    log.info("Terminate service DirectorySizeChecker");
  }
}