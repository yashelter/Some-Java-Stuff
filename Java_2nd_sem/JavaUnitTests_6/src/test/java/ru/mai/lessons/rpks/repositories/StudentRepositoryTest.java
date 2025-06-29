package ru.mai.lessons.rpks.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.mai.lessons.rpks.models.Student;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Optional;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StudentRepositoryTest {

  private Student student1 = new Student(null, "Sharp", "Best");
  private Student student2 = new Student(null, "Java", "Not");

  @Autowired
  private StudentRepository repository;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
  }

  @Test
  @DisplayName("Тест на поиск студента по его идентификатору")
  void givenStudent_whenFindById_thenReturnStudent() {
    Student studentToSave = new Student(null, "Domoroschenov", "М8О-411Б");
    Student savedStudent = repository.save(studentToSave);

    Student studentById = repository.findById(savedStudent.getId()).orElse(null);

    assertNotNull(studentById);
    assertEquals(studentToSave.getFullName(), studentById.getFullName());
    assertEquals(studentToSave.getGroupName(), studentById.getGroupName());
  }

  @Test
  @DisplayName("Успешное сохранение студента и присвоение ID")
  void saveStudent_ShouldPersistStudentAndAssignId() {
    Student studentToSave = new Student(null, "Sergey Ivanov", "М8О-410Б-19");
    Student savedStudent = repository.save(studentToSave);

    assertNotNull(savedStudent);
    assertNotNull(savedStudent.getId(), "ID должен быть сгенерирован после сохранения");
    assertEquals("Sergey Ivanov", savedStudent.getFullName());
    assertEquals("М8О-410Б-19", savedStudent.getGroupName());
  }

  @Test
  @DisplayName("Тест на поиск студента по его существующему идентификатору")
  void givenStudentExists_whenFindById_thenReturnStudent() {
    Student savedStudent1 = repository.save(student1); // student1 gets an ID here

    Optional<Student> foundStudentOpt = repository.findById(savedStudent1.getId());

    assertTrue(foundStudentOpt.isPresent(), "Студент должен быть найден");
    Student foundStudent = foundStudentOpt.get();
    assertEquals(savedStudent1.getId(), foundStudent.getId());
    assertEquals(student1.getFullName(), foundStudent.getFullName());
    assertEquals(student1.getGroupName(), foundStudent.getGroupName());
  }

  @Test
  @DisplayName("Тест на поиск студента по несуществующему идентификатору")
  void givenStudentDoesNotExist_whenFindById_thenReturnEmptyOptional() {
    Optional<Student> foundStudentOpt = repository.findById(999L); // Assuming 999L is a non-existent ID

    assertFalse(foundStudentOpt.isPresent(), "Студент не должен быть найден");
  }

  @Test
  @DisplayName("Тест на получение всех студентов, когда они существуют")
  void givenStudentsExist_whenFindAll_thenReturnAllStudents() {
    repository.save(student1);
    repository.save(student2);

    List<Student> students = repository.findAll();

    assertNotNull(students);
    assertEquals(2, students.size(), "Должно быть найдено 2 студента");
    // You could add more specific checks here if needed, e.g., checking contents
    assertTrue(students.stream().anyMatch(s -> s.getFullName().equals(student1.getFullName())));
    assertTrue(students.stream().anyMatch(s -> s.getFullName().equals(student2.getFullName())));
  }

  @Test
  @DisplayName("Тест на получение всех студентов, когда их нет в базе")
  void givenNoStudentsExist_whenFindAll_thenReturnEmptyList() {
    List<Student> students = repository.findAll();

    assertNotNull(students);
    assertTrue(students.isEmpty(), "Список студентов должен быть пустым");
  }

  @Test
  @DisplayName("Успешное обновление данных студента")
  void givenStudentExists_whenUpdateStudent_thenChangesShouldBePersisted() {
    Student savedStudent = repository.save(student1);
    assertNotNull(savedStudent.getId());

    Student studentToUpdate = repository.findById(savedStudent.getId()).orElseThrow();
    studentToUpdate.setFullName("Ivan Petrov Updated");
    studentToUpdate.setGroupName("М8О-303Б-21");
    repository.save(studentToUpdate); // save() also performs update if ID exists

    Student updatedStudent = repository.findById(savedStudent.getId()).orElseThrow();
    assertEquals("Ivan Petrov Updated", updatedStudent.getFullName());
    assertEquals("М8О-303Б-21", updatedStudent.getGroupName());
  }


  @Test
  @DisplayName("Успешное удаление студента по ID")
  void givenStudentExists_whenDeleteById_thenStudentShouldBeRemoved() {
    Student savedStudent = repository.save(student1);
    Long studentId = savedStudent.getId();
    assertNotNull(studentId);
    assertTrue(repository.existsById(studentId), "Студент должен существовать перед удалением");

    repository.deleteById(studentId);

    assertFalse(repository.existsById(studentId), "Студент не должен существовать после удаления");
  }

  @Test
  @DisplayName("Успешное удаление всех студентов")
  void givenStudentsExist_whenDeleteAll_thenAllStudentsShouldBeRemoved() {
    repository.save(student1);
    repository.save(student2);
    assertEquals(2, repository.count(), "В базе должно быть 2 студента перед deleteAll");

    repository.deleteAll();

    assertEquals(0, repository.count(), "Все студенты должны быть удалены");
  }

  @Test
  @DisplayName("Проверка существования студента по ID (existsById)")
  void givenStudentExists_whenExistsById_thenReturnTrue() {
    Student savedStudent = repository.save(student1);
    assertTrue(repository.existsById(savedStudent.getId()));
  }

  @Test
  @DisplayName("Проверка отсутствия студента по ID (existsById)")
  void givenStudentDoesNotExist_whenExistsById_thenReturnFalse() {
    assertFalse(repository.existsById(999L));
  }

  @Test
  @DisplayName("Проверка количества записей (count)")
  void givenStudentsExist_whenCount_thenReturnCorrectNumber() {
    assertEquals(0, repository.count());
    repository.save(student1);
    assertEquals(1, repository.count());
    repository.save(student2);
    assertEquals(2, repository.count());
  }
}