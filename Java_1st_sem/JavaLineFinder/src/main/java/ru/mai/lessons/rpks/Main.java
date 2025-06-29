package ru.mai.lessons.rpks;

import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.exception.LineCountShouldBePositiveException;
import ru.mai.lessons.rpks.impl.LineFinder;

import java.util.Scanner;

@Slf4j
public class Main {
  public static void main(String[] args) throws LineCountShouldBePositiveException {
    log.info("Start service LineFinder");

    Scanner in = new Scanner(System.in);
    log.info("Введите ключевое слово для поиска: ");
    String keyWord = in.nextLine();
    log.info("Введите количество строк до и после найденной строки для записи в результирующий "
             + "файл: ");
    int lineCount = in.nextInt();

    ILineFinder service = new LineFinder(); // ваша реализация service
    long startTime = System.currentTimeMillis();
    service.find(args[0], args[1], keyWord, lineCount);
    log.info("Поиск отработал за {} ms.", System.currentTimeMillis() - startTime);

    log.info("Terminate service LineFinder");
  }
}