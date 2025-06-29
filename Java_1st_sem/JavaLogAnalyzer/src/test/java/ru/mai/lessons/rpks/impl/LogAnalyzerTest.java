package ru.mai.lessons.rpks.impl;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ru.mai.lessons.rpks.ILogAnalyzer;
import ru.mai.lessons.rpks.exception.WrongFilenameException;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

public class LogAnalyzerTest {
  private static final String LOG_FILENAME = "log.txt";

  private ILogAnalyzer logAnalyzer;

  @BeforeClass
  public void setUp() {
    logAnalyzer = new LogAnalyzer();
  }

  @DataProvider(name = "validDeviationCases")
  private Object[][] getValidDeviationCases() {
    return new Object[][] {
        {"3 sec", List.of(2, 6, 8, 11, 14)},
        {"1 min", List.of(14)}
    };
  }

  @Test(dataProvider = "validDeviationCases",
        description = "Проверяем границы отклонения от медианы")
  public void testPositiveAnalyze(String deviation, List<Integer> expectedErrorQueryIds)
      throws WrongFilenameException {
    // WHEN
    List<Integer> actualErrorQueryIds = logAnalyzer.analyze(LOG_FILENAME, deviation);

    // THEN
    assertEquals(actualErrorQueryIds, expectedErrorQueryIds);
  }

  @Test(description = "Проверяем, что при отсутствии параметра отклонения сервис должен "
                      + "самостоятельно выявить аномальные запросы")
  public void testPositiveAnalyzeAnomalyForAbsentDeviationParameter() {
    try {
      // GIVEN
      String deviation = "";

      // WHEN
      List<Integer> actualErrorQueryIds = logAnalyzer.analyze(LOG_FILENAME, deviation);

      // THEN
      assertFalse(actualErrorQueryIds.isEmpty());
    } catch (Exception e) {
      fail("Не должны были получить исключение", e);
    }
  }

  @DataProvider(name = "wrongFilenameCases")
  private Object[][] getWrongFilenameCases() {
    return new Object[][] {
        {""},
        {"unknown_file.txt"}
    };
  }

  @Test(dataProvider = "wrongFilenameCases",
        expectedExceptions = WrongFilenameException.class,
        description = "Проверяем реакцию сервиса на ошибку при работе с файлом")
  public void testNegativeWrongFilename(String errorFilename) throws WrongFilenameException {
    // GIVEN
    String deviation = "3 sec";

    // WHEN
    logAnalyzer.analyze(errorFilename, deviation);

    // THEN ожидаем получение исключения
  }
}