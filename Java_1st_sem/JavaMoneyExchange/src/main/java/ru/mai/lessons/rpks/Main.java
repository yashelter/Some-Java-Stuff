package ru.mai.lessons.rpks;

import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.exception.ExchangeIsImpossibleException;
import ru.mai.lessons.rpks.impl.MoneyExchange;

import java.util.Scanner;

@Slf4j
public class Main {
    public static void main(String[] args) throws ExchangeIsImpossibleException {
        log.info("Запуск сервиса MoneyExchange");

        Scanner in = new Scanner(System.in);
        log.info("Введите сумму, которую нужно разменять: ");
        Integer sum = Integer.parseInt(in.nextLine());
        log.info("Введите номинал монет (через запятую): ");
        String coinDenomination = in.nextLine();

        IMoneyExchange service = new MoneyExchange();

        System.out.println(service.exchange(sum, coinDenomination));
    }
}