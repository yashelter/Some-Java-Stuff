package ru.mai.lessons.rpks.impl;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ru.mai.lessons.rpks.IMoneyExchange;
import ru.mai.lessons.rpks.exception.ExchangeIsImpossibleException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class MoneyExchangeTest {
  private IMoneyExchange moneyExchange;

  @BeforeClass
  public void setUp() {
    moneyExchange = new MoneyExchange();
  }

  @DataProvider(name = "validCases", parallel = true)
  private Object[][] getValidCases() {
    return new Object[][] {
        {10, "10", "10[1]"},
        {10, "4, 1", "4[2], 1[2]"},
        {10, "1, 4", "4[2], 1[2]"},
        {10, "5, 1", "5[2]"},
        {10, "0, 1, 5", "5[2]"},
        {10, "-1, 5", "5[2]"},
        {10, "7, 2, 1", "7[1], 2[1], 1[1]"},
        {24, "5, 3, 1", "5[4], 3[1], 1[1]"},
        {10, "3, 2", "3[2], 2[2]"}
    };
  }

  @Test(dataProvider = "validCases", description = "Успешный размен монет")
  public void testPositiveExchange(Integer sumToExchange,
                                   String coinDenomination,
                                   String expectedResult) throws ExchangeIsImpossibleException {
    // WHEN
    String actualResult = moneyExchange.exchange(sumToExchange, coinDenomination);

    // THEN
    assertNotNull(actualResult);
    assertEquals(actualResult, expectedResult);
  }

  @DataProvider(name = "invalidCases", parallel = true)
  private Object[][] getInvalidCases() {
    return new Object[][] {
        {10, "0"},
        {10, "11"},
        {10, ""},
        {10, "-1"}
    };
  }

  @Test(dataProvider = "invalidCases",
        expectedExceptions = ExchangeIsImpossibleException.class,
        description = "Размен монет невозможен")
  public void testNegativeExchange(Integer sumToExchange, String coinDenomination)
      throws ExchangeIsImpossibleException {
    // WHEN
    moneyExchange.exchange(sumToExchange, coinDenomination);

    // THEN ожидаем получение исключения
  }
}
