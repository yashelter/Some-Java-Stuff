package ru.mai.lessons.rpks;

import ru.mai.lessons.rpks.exception.ExchangeIsImpossibleException;

public interface IMoneyExchange {
  public String exchange(Integer sum, String coinDenomination)
      throws ExchangeIsImpossibleException; // запускает размен монет
}
