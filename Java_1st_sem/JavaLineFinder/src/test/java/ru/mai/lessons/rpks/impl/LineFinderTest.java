package ru.mai.lessons.rpks.impl;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ru.mai.lessons.rpks.ILineFinder;
import ru.mai.lessons.rpks.exception.LineCountShouldBePositiveException;

@Slf4j
public class LineFinderTest {

  private static final String INPUT_FILENAME = getPath("inputFile.txt").toString();
  private static final String EXPECTED_CORGI_OUTPUT_FILENAME = "expectedCorgiOutputFile.txt";
  private static final String EXPECTED_BOAST_1_LINE_FILENAME = "expectedBoastOutputFile1.txt";
  private static final String EXPECTED_BOAST_2_LINES_FILENAME = "expectedBoastOutputFile2.txt";
  private static final String EXPECTED_BOAST_3_LINES_FILENAME = "expectedBoastOutputFile3.txt";
  private static final String EXPECTED_HANDS_1_LINE_FILENAME = "expectedHandsOutputFile1.txt";
  private static final String EXPECTED_NEVER_1_LINE_FILENAME = "expectedNeverOutputFile1.txt";

  private ILineFinder lineFinder;

  @BeforeClass
  public void setUp() {
    lineFinder = new LineFinder();
  }

  @Test(description = "Проверяем успешный поиск строки в файле по заданному ключевому слову")
  public void testPositiveFind()
      throws IOException, LineCountShouldBePositiveException {
    // GIVEN
    String keyWord = "корги";
    int lineCount = 0;
    String outputFilename = getOutputFilename(keyWord, lineCount);

    // WHEN
    long startTime = System.currentTimeMillis();
    lineFinder.find(INPUT_FILENAME, outputFilename, keyWord, lineCount);
    log.info("Поиск отработал за {} ms.", System.currentTimeMillis() - startTime);

    // THEN
    assertEquals(Files.readAllLines(Paths.get(outputFilename)),
        Files.readAllLines(getPath(EXPECTED_CORGI_OUTPUT_FILENAME)));
  }

  @DataProvider(name = "linesNumberFromStart")
  private Object[][] getLinesNumberFromStart() {
    return new Object[][]{
        {1, EXPECTED_BOAST_1_LINE_FILENAME},
        {2, EXPECTED_BOAST_2_LINES_FILENAME},
        {3, EXPECTED_BOAST_3_LINES_FILENAME}
    };
  }

  @Test(dataProvider = "linesNumberFromStart",
      description = "Проверяем успешный вывод указанного количества строк до и после найденной "
          + "строки. Проверяем поиск в начале текста.")
  public void testPositiveFindStartMultipleLines(int lineCount, String expectedFilename)
      throws IOException, LineCountShouldBePositiveException {
    // GIVEN
    String keyWord = "похвастаться";
    String outputFilename = getOutputFilename(keyWord, lineCount);

    // WHEN
    long startTime = System.currentTimeMillis();
    lineFinder.find(INPUT_FILENAME, outputFilename, keyWord, lineCount);
    log.info("Поиск отработал за {} ms.", System.currentTimeMillis() - startTime);

    // THEN
    assertEquals(Files.readAllLines(Paths.get(outputFilename)),
        Files.readAllLines(getPath(expectedFilename)));
  }

  @Test(description = "Проверяем успешный вывод указанного количества строк до и после найденной "
      + "строки. Проверяем поиск в конце текста.")
  public void testPositiveFindMultipleLinesAtTheEnd()
      throws IOException, LineCountShouldBePositiveException {
    // GIVEN
    String keyWord = "руки";
    int lineCount = 1;
    String outputFilename = getOutputFilename(keyWord, lineCount);

    // WHEN
    long startTime = System.currentTimeMillis();
    lineFinder.find(INPUT_FILENAME, outputFilename, keyWord, lineCount);
    log.info("Поиск отработал за {} ms.", System.currentTimeMillis() - startTime);

    // THEN
    assertEquals(Files.readAllLines(Paths.get(outputFilename)),
        Files.readAllLines(getPath(EXPECTED_HANDS_1_LINE_FILENAME)));
  }

  @Test(description = "Проверяем успешный вывод указанного количества строк до и после нескольких "
      + "найденных строк. Проверяем поиск в середине текста.")
  public void testPositiveFindMiddleMultipleLines()
      throws IOException, LineCountShouldBePositiveException {
    // GIVEN
    String keyWord = "никогда";
    int lineCount = 1;
    String outputFilename = getOutputFilename(keyWord, lineCount);

    // WHEN
    long startTime = System.currentTimeMillis();
    lineFinder.find(INPUT_FILENAME, outputFilename, keyWord, lineCount);
    log.info("Поиск отработал за {} ms.", System.currentTimeMillis() - startTime);

    // THEN
    assertEquals(Files.readAllLines(Paths.get(outputFilename)),
        Files.readAllLines(getPath(EXPECTED_NEVER_1_LINE_FILENAME)));
  }

  @DataProvider(name = "linesNotFoundCases")
  private Object[][] getLinesNotFoundCases() {
    return new Object[][]{
        {"кошка"},
        {""}
    };
  }

  @Test(dataProvider = "linesNotFoundCases",
      description = "Проверяем корректную обработку текста, когда не найдено ни одной строки. "
          + "Ожидаем, что созданный файл пустой.")
  public void testPositiveWordNotFound(String keyWord) throws LineCountShouldBePositiveException {
    // GIVEN
    int lineCount = 1;
    String outputFilename = getOutputFilename(keyWord, lineCount);

    // WHEN
    long startTime = System.currentTimeMillis();
    lineFinder.find(INPUT_FILENAME, outputFilename, keyWord, lineCount);
    log.info("Поиск отработал за {} ms.", System.currentTimeMillis() - startTime);

    // THEN
    assertEquals(new File(outputFilename).length(), 0);
  }

  @Test(expectedExceptions = LineCountShouldBePositiveException.class,
      description = "Проверяем валидацию значения lineCount")
  public void testNegativeLineCountValue() throws LineCountShouldBePositiveException {
    // GIVEN
    String keyWord = "королева";
    int lineCount = -1;
    String outputFilename = getOutputFilename(keyWord, lineCount);

    // WHEN
    lineFinder.find(INPUT_FILENAME, outputFilename, keyWord, lineCount);

    // THEN ожидаем получение исключения
  }

  //region Вспомогательные методы
  private String getOutputFilename(String keyWord, int lineCount) {
    return String.format("outputFilename_%s_%d_lines.txt", keyWord, lineCount);
  }

  private static Path getPath(String filename) {
    try {
      return Paths.get(
          Objects.requireNonNull(LineFinderTest.class.getClassLoader().getResource(filename))
              .toURI());
    } catch (Exception ex) {
      return Path.of("");
    }
  }
  //endregion
}