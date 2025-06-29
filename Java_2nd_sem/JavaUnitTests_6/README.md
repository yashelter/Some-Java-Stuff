# Лабораторная работа №8 - JavaUnitTests

## Лабораторная работа считается готовой к сдаче, если выполнены следующие действия

### ВНИМАНИЕ: Все креды, эндпоинты, хосты, порты и т.п. в настройках приложения необходимо хранить в виде переменной окружения. А также нужно соблюдать регламент именований UNIT тестов и их написания. В каждом файле для тестов есть примеры.

### 1. Написать тесты на слой контроллеров
- В проекте есть один контроллер - StudentController, который необходимо покрыть UNIT тестами
- Каждый метод контроллера должен быть покрыт тестами
- Каждый метод должен иметь негативный сценарий в тестировании
- Пример теста:
  ```java
    @Test
    @SneakyThrows
    @DisplayName("Тест на поиск студента по его идентификатору")
    void givenStudentId_whenGetStudent_thenReturnStudentResponse() {
      StudentResponse expectedResponse = new StudentResponse();
      when(service.getStudent(1L)).thenReturn(expectedResponse);
  
      mockMvc
          .perform(
              get("/student/get")
                  .param("id", "1")
          )
          .andExpect(status().isOk())
          .andExpect(content().string(JsonUtils.toJson(expectedResponse)));
    }
  ```

### 2. Написать тесты на слой репозиториев
- В проекте есть один репозиторий - StudentRepository, который необходимо покрыть UNIT тестами
- Покрыты тестами должны быть базовые методы CRUD
- Для написания UNIT тестов на слой репозитория необходимо использовать DataJpaTest и тестовый конфигурационный файл приложения
- Пример теста:
  ```java
    @Test
    @DisplayName("Тест на поиск студента по его идентификатору")
    void givenStudent_whenFindById_thenReturnStudent() {
      Student studentToSave = new Student(null, "Domoroschenov", "М8О-411Б");
      repository.save(studentToSave);
  
      Student studentById = repository.findById(1L).orElse(null);
  
      assertNotNull(studentById);
      assertEquals(studentToSave.getFullName(), studentById.getFullName());
      assertEquals(studentToSave.getGroupName(), studentById.getGroupName());
    }
  ```

### 3. Написать тесты на слой сервисов
- В проекте есть один сервис - StudentService, который необходимо покрыть UNIT тестами
- Каждый метод сервиса должен быть покрыт тестами
- Каждый метод должен иметь негативный сценарий в тестировании
- Пример теста:
  ```java
    @Test
    @DisplayName("Тест на поиск студента по его идентификатору")
    void givenStudentId_whenGetStudent_thenReturnStudentResponse() {
      Long studentId = 1L;
      StudentResponse expectedResponse = new StudentResponse(1L, "Domoroschenov", "М8О-411Б");
      Student expectedModel = new Student(1L, "Domoroschenov", "М8О-411Б");
      when(repository.findById(studentId)).thenReturn(Optional.of(expectedModel));
      when(mapper.modelToResponse(expectedModel)).thenReturn(expectedResponse);
  
      StudentResponse actualResponse = service.getStudent(studentId);
  
      assertEquals(expectedResponse, actualResponse);
    }
  ```

### 4. Соблюдать регламент написания UNIT тестов
- Каждый тест должен тестировать ТОЛЬКО 1 функционал
- Каждый тест должен иметь 3 секции - подготовка данных, исполнение функционала, сравнение результата с ожидаемым
- Наименование тестов должны корректно описывать сценарий и также иметь 3 секции - какие данные даны, какой функционал исполняется, что ожидается
- Каждый тест должен иметь аннотацию @DisplayName, которая корректно описывает тест

## Лабораторная работа сдана, если:
1. Все unit-тесты и интеграционные тесты пройдены. Для запуска интеграционных тестов должен быть установлен Docker Desktop (альтернативы).
2. SonarLint в отчёте не выдает code smells (Правая кнопка мыши на проект -> SonarLint -> Analyze with SonarLint -> Proceed). Должен быть в IDEA установлен плагин SonarLint.
3. Приложение не выдает необработанных ошибок.
4. Нет похожего кода у других студентов.

## Процесс выполнения лабораторной работы:
1. Клонируете себе репозиторий лабораторной работы.
2. Создаете от ветки develop ветку с именем student/фамилия_перваябукваимени_lab1. Убедитесь, что не выполняете коммиты и пуши в develop!!!
3. Пишите код.
4. Все переменные окружения в качестве примера берете из config.env.
5. Запускаете все тесты, все тесты должны быть пройдены.
6. Исправляете code smells от SonarLint - не должно быть ни одного code smells
7. Когда считаете, что лабораторная работа готова к проверке создаете pull request в ветку release и ставите label
   "Можно проверять лабораторную работу"
8. По pull request происходит проверка кода и оставляются замечания в коде, которые надо поправить, у pull request будет label "Нужны правки"
9. Если лабораторная работа принята, то у pull request будет label "Лабораторная работа зачтена"
10. Для pull request не нажимаем кнопку merge и close после того, как лабораторная работа зачтена!!!
