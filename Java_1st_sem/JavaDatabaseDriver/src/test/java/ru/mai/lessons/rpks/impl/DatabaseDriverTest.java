package ru.mai.lessons.rpks.impl;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ru.mai.lessons.rpks.IDatabaseDriver;
import ru.mai.lessons.rpks.exception.FieldNotFoundInTableException;
import ru.mai.lessons.rpks.exception.WrongCommandFormatException;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class DatabaseDriverTest {
  private static final String GRADE_FILENAME = "grade.csv";
  private static final String GROUPS_FILENAME = "groups.csv";
  private static final String STUDENTS_FILENAME = "students.csv";
  private static final String SUBJECTS_FILENAME = "subjects.csv";

  private IDatabaseDriver databaseDriver;

  @BeforeClass
  public void setUp() {
    databaseDriver = new DatabaseDriver();
  }

  @DataProvider(name = "selectFromCases")
  private Object[][] getSelectFromCase() {
    return new Object[][] {
        {"SELECT=full_name FROM=" + STUDENTS_FILENAME,
         List.of("Иванов Иван", "Петров Олег", "Игнатова Ольга", "Сидоров Николай",
                 "Калинина Дарья", "Кузнецов Михаил", "Орлов Виктор", "Никитина Ирина")},
        {"SELECT=id,full_name FROM=" + STUDENTS_FILENAME,
         List.of("0;Иванов Иван", "1;Петров Олег", "2;Игнатова Ольга", "3;Сидоров Николай",
                 "4;Калинина Дарья", "5;Кузнецов Михаил", "6;Орлов Виктор", "7;Никитина Ирина")},
        {"SELECT=full_name,id FROM=" + STUDENTS_FILENAME,
         List.of("Иванов Иван;0", "Петров Олег;1", "Игнатова Ольга;2", "Сидоров Николай;3",
                 "Калинина Дарья;4", "Кузнецов Михаил;5", "Орлов Виктор;6", "Никитина Ирина;7")},
        {"SELECT=subject_name FROM=" + SUBJECTS_FILENAME,
         List.of("РПКС", "Матан", "История", "Английский")},
        {"SELECT=group_name,student_id FROM=" + GROUPS_FILENAME,
         List.of("5ИНТ-001;5", "5ИНТ-002;2", "5ПМИ-001;4", "5ИНТ-001;0", "5ИНТ-001;1", "5ИНТ-002;7",
                 "5ПМИ-001;3", "5ПМИ-001;6")}
    };
  }

  @Test(dataProvider = "selectFromCases",
        description = "Проверяем успешное выполнение простых запросов SELECT+FROM")
  public void testPositiveFindDataBySimpleSelectFrom(String command, List<String> expectedResult)
      throws FieldNotFoundInTableException, WrongCommandFormatException {
    // WHEN
    List<String> actualResult = databaseDriver.find(STUDENTS_FILENAME, GROUPS_FILENAME,
                                                    SUBJECTS_FILENAME, GRADE_FILENAME, command);

    // THEN
    assertEquals(actualResult, expectedResult);
  }

  @DataProvider(name = "selectFromWhereCases")
  private Object[][] getSelectFromWhereCase() {
    return new Object[][] {
        {
            String.format(
                "SELECT=group_name,grade,date FROM=%s,%s,%s,%s WHERE=(full_name='%s' AND subject_name='%s')",
                STUDENTS_FILENAME, GROUPS_FILENAME, SUBJECTS_FILENAME, GRADE_FILENAME,
                "Иванов Иван", "Английский"),
            List.of("5ИНТ-001;5;17.01.2023")
        },
        {
            String.format(
                "SELECT=full_name,group_name,grade,date FROM=%s,%s,%s,%s WHERE=(subject_name='%s')",
                STUDENTS_FILENAME, GROUPS_FILENAME, SUBJECTS_FILENAME, GRADE_FILENAME, "РПКС"),
            List.of("Иванов Иван;5ИНТ-001;5;05.01.2023", "Петров Олег;5ИНТ-001;4;05.01.2023",
                    "Игнатова Ольга;5ИНТ-002;4;06.01.2023", "Сидоров Николай;5ПМИ-001;5;07.01.2023",
                    "Калинина Дарья;5ПМИ-001;3;07.01.2023", "Кузнецов Михаил;5ИНТ-001;5;05.01.2023",
                    "Орлов Виктор;5ПМИ-001;4;07.01.2023", "Никитина Ирина;5ИНТ-002;3;06.01.2023")
        },
        {
            String.format(
                "SELECT=full_name,subject_name FROM=%s,%s,%s WHERE=(grade='3' OR grade='4' OR grade='2')",
                STUDENTS_FILENAME, SUBJECTS_FILENAME, GRADE_FILENAME),
            List.of("Иванов Иван;Матан", "Петров Олег;РПКС", "Петров Олег;Матан",
                    "Петров Олег;История", "Игнатова Ольга;РПКС", "Калинина Дарья;РПКС",
                    "Калинина Дарья;Матан", "Калинина Дарья;История", "Калинина Дарья;Английский",
                    "Кузнецов Михаил;Матан", "Кузнецов Михаил;История",
                    "Кузнецов Михаил;Английский", "Орлов Виктор;РПКС", "Орлов Виктор;Матан",
                    "Орлов Виктор;История", "Орлов Виктор;Английский", "Никитина Ирина;РПКС",
                    "Никитина Ирина;Матан", "Никитина Ирина;Английский")
        },
        {
            String.format(
                "SELECT=group_name,full_name,subject_name FROM=%s,%s,%s,%s WHERE=(grade='2')",
                STUDENTS_FILENAME, GROUPS_FILENAME, SUBJECTS_FILENAME, GRADE_FILENAME),
            List.of("5ИНТ-002;Никитина Ирина;Английский")
        },
        {
            String.format("SELECT=full_name,subject_name FROM=%s,%s,%s WHERE=(grade='1')",
                          STUDENTS_FILENAME, SUBJECTS_FILENAME, GRADE_FILENAME),
            List.of("")
        }
    };
  }

  @Test(dataProvider = "selectFromWhereCases",
        description = "Проверяем успешное выполнение запросов с условием SELECT+FROM+WHERE")
  public void testPositiveFindDataBySelectFromWhere(String command, List<String> expectedResult)
      throws FieldNotFoundInTableException, WrongCommandFormatException {
    // WHEN
    List<String> actualResult = databaseDriver.find(STUDENTS_FILENAME, GROUPS_FILENAME,
                                                    SUBJECTS_FILENAME, GRADE_FILENAME, command);

    // THEN
    assertEquals(actualResult, expectedResult);
  }

  @DataProvider(name = "selectFromWhereGroupByCases")
  private Object[][] getSelectFromWhereGroupByCase() {
    return new Object[][] {
        {
            String.format("SELECT=group_name FROM=%s GROUPBY=group_name", GROUPS_FILENAME),
            List.of("5ИНТ-001", "5ИНТ-002", "5ПМИ-001")
        },
        {
            String.format("SELECT=subject_name FROM=%s,%s WHERE=(grade='5') GROUPBY=subject_name",
                          SUBJECTS_FILENAME, GRADE_FILENAME),
            List.of("РПКС", "История", "Английский", "Матан")
        }
    };
  }

  @Test(dataProvider = "selectFromWhereGroupByCases",
        description = "Проверяем успешное выполнение запросов с группировкой SELECT+FROM+WHERE+GROUPBY")
  public void testPositiveFindDataBySelectFromWhereGroupBy(String command,
                                                           List<String> expectedResult)
      throws FieldNotFoundInTableException, WrongCommandFormatException {
    // WHEN
    List<String> actualResult = databaseDriver.find(STUDENTS_FILENAME, GROUPS_FILENAME,
                                                    SUBJECTS_FILENAME, GRADE_FILENAME, command);

    // THEN
    assertEquals(actualResult, expectedResult);
  }

  @Test(expectedExceptions = FieldNotFoundInTableException.class,
        description = "Проверяем реакцию на попытку получить данные из поля, которого нет в таблице")
  public void testNegativeTryFindUnknownFieldInTable()
      throws FieldNotFoundInTableException, WrongCommandFormatException {
    // GIVEN
    String command = "SELECT=group_name FROM=" + STUDENTS_FILENAME;

    // WHEN
    databaseDriver.find(STUDENTS_FILENAME, GROUPS_FILENAME, SUBJECTS_FILENAME, GRADE_FILENAME,
                        command);
    // THEN ожидаем получение исключения
  }

  @DataProvider(name = "wrongFormatCommandCases")
  private Object[][] getWrongFormatCommandCase() {
    return new Object[][] {
        {"SELECT group_name FROM=" + STUDENTS_FILENAME},
        {"SELECT=group_name FROM " + STUDENTS_FILENAME},
        {"SELECT=group_name"},
        {"FROM=" + STUDENTS_FILENAME},
        {String.format("SELECT= FROM=%s GROUPBY=group_name", GROUPS_FILENAME)},
        {"SELECT=group_name FROM= GROUPBY=group_name"},
        {String.format("SELECT=group_name FROM=%s GROUPBY=", GROUPS_FILENAME)},
        {String.format("SELECT=group_name FROM=%s GROUPBY group_name", GROUPS_FILENAME)},
        {String.format("SELECT=full_name,subject_name FROM=%s,%s %s WHERE=(grade='5')",
                       STUDENTS_FILENAME, SUBJECTS_FILENAME, GRADE_FILENAME)},
        {String.format("SELECT=full_name,subject_name FROM=%s,%s,%s WHERE (grade='5')",
                       STUDENTS_FILENAME, SUBJECTS_FILENAME, GRADE_FILENAME)},
        {String.format("SELECT=full_name,subject_name FROM=%s,%s,%s WHERE=grade='5'",
                       STUDENTS_FILENAME, SUBJECTS_FILENAME, GRADE_FILENAME)},
        {String.format("SELECT=full_name,subject_name FROM=%s,%s,%s WHERE=(grade=='5')",
                       STUDENTS_FILENAME, SUBJECTS_FILENAME, GRADE_FILENAME)}
    };
  }

  @Test(dataProvider = "wrongFormatCommandCases",
        expectedExceptions = WrongCommandFormatException.class,
        description = "Проверяем валидацию формата команды")
  public void testNegativeTryFindUnknownFieldInTable(String wrongFormatCommand)
      throws FieldNotFoundInTableException, WrongCommandFormatException {
    // WHEN
    databaseDriver.find(STUDENTS_FILENAME, GROUPS_FILENAME, SUBJECTS_FILENAME, GRADE_FILENAME,
                        wrongFormatCommand);
    // THEN ожидаем получение исключения
  }
}