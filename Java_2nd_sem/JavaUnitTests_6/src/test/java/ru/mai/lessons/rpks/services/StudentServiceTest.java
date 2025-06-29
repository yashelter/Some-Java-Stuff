package ru.mai.lessons.rpks.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.webjars.NotFoundException;
import ru.mai.lessons.rpks.dto.mappers.StudentMapper;
import ru.mai.lessons.rpks.dto.requests.StudentCreateRequest;
import ru.mai.lessons.rpks.dto.requests.StudentUpdateRequest;
import ru.mai.lessons.rpks.dto.respones.StudentResponse;
import ru.mai.lessons.rpks.models.Student;
import ru.mai.lessons.rpks.repositories.StudentRepository;
import ru.mai.lessons.rpks.services.impl.StudentServiceImpl;


@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

  @Mock
  private StudentRepository repository;

  @Mock
  private StudentMapper mapper;

  @InjectMocks
  private StudentServiceImpl service;

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

  @Test
  @DisplayName("getStudent: Успешный поиск студента по ID")
  void getStudent_whenStudentExists_thenReturnStudentResponse() {
    Long studentId = 1L;
    Student studentModel = new Student(studentId, "Chell", "Test Subject");
    StudentResponse expectedResponse = new StudentResponse(studentId, "Chell", "Test Subject");

    when(repository.findById(studentId)).thenReturn(Optional.of(studentModel));
    when(mapper.modelToResponse(studentModel)).thenReturn(expectedResponse);

    StudentResponse actualResponse = service.getStudent(studentId);

    assertEquals(expectedResponse, actualResponse);
    verify(repository, times(1)).findById(studentId);
    verify(mapper, times(1)).modelToResponse(studentModel);
  }

  @Test
  @DisplayName("getStudent: Студент не найден по ID, выбрасывается NotFoundException")
  void getStudent_whenStudentNotFound_thenThrowNotFoundException() {
    Long studentId = 404L;

    when(repository.findById(studentId)).thenReturn(Optional.empty());

    NotFoundException exception = assertThrows(NotFoundException.class, () -> service.getStudent(studentId));

    assertEquals("Студент не найден", exception.getMessage());
    verify(repository, times(1)).findById(studentId);
    verify(mapper, never()).modelToResponse(any());
  }

  @Test
  @DisplayName("getStudent: Ошибка при маппинге модели в DTO")
  void getStudent_whenMappingFails_thenThrowRuntimeException() {
    Long studentId = 7L;
    Student studentModel = new Student(studentId, "Fact Core", "Core");

    when(repository.findById(studentId)).thenReturn(Optional.of(studentModel));
    when(mapper.modelToResponse(studentModel)).thenThrow(new RuntimeException(""));

    assertThrows(RuntimeException.class, () -> service.getStudent(studentId));

    verify(repository, times(1)).findById(studentId);
    verify(mapper, times(1)).modelToResponse(studentModel);
  }


  @Test
  @DisplayName("saveStudent: Успешное сохранение нового студента")
  void saveStudent_whenValidRequest_thenReturnSavedStudentResponse() {
    StudentCreateRequest createRequest = new StudentCreateRequest("GLaDOS", "Core");
    Student studentToSave = new Student(null, "GLaDOS", "Core");
    Student savedStudent = new Student(10L, "GLaDOS", "Core");
    StudentResponse expectedResponse = new StudentResponse(10L, "GLaDOS", "Core");

    when(mapper.requestToModel(createRequest)).thenReturn(studentToSave);
    when(repository.saveAndFlush(studentToSave)).thenReturn(savedStudent);
    when(mapper.modelToResponse(savedStudent)).thenReturn(expectedResponse);

    StudentResponse actualResponse = service.saveStudent(createRequest);

    assertEquals(expectedResponse, actualResponse);
    verify(mapper, times(1)).requestToModel(createRequest);
    verify(repository, times(1)).saveAndFlush(studentToSave);
    verify(mapper, times(1)).modelToResponse(savedStudent);
  }

  @Test
  @DisplayName("saveStudent: Ошибка при сохранении в репозиторий (например, нарушение уникальности)")
  void saveStudent_whenRepositorySaveFails_thenThrowException() {
    StudentCreateRequest createRequest = new StudentCreateRequest("Atlas", "Core");
    Student studentToSave = new Student(null, "Atlas", "Core");

    when(mapper.requestToModel(createRequest)).thenReturn(studentToSave);
    when(repository.saveAndFlush(studentToSave)).thenThrow(new DataIntegrityViolationException("Duplicate entry"));

    assertThrows(DataIntegrityViolationException.class, () -> service.saveStudent(createRequest));

    verify(mapper, times(1)).requestToModel(createRequest);
    verify(repository, times(1)).saveAndFlush(studentToSave);
    verify(mapper, never()).modelToResponse(any());
  }


  @Test
  @DisplayName("updateStudent: Успешное обновление существующего студента")
  void updateStudent_whenValidRequest_thenReturnUpdatedStudentResponse() {
    Long studentIdToUpdate = 2L;
    StudentUpdateRequest updateRequest = new StudentUpdateRequest(studentIdToUpdate, "Wheatley", "Core");
    Student studentToUpdate = new Student(studentIdToUpdate, "Wheatley", "Core");
    Student updatedStudentFromRepo = new Student(studentIdToUpdate, "Wheatley", "Core");
    StudentResponse expectedResponse = new StudentResponse(studentIdToUpdate, "Wheatley", "Core");

    when(mapper.requestToModel(updateRequest)).thenReturn(studentToUpdate);
    when(repository.saveAndFlush(studentToUpdate)).thenReturn(updatedStudentFromRepo);
    when(mapper.modelToResponse(updatedStudentFromRepo)).thenReturn(expectedResponse);

    StudentResponse actualResponse = service.updateStudent(updateRequest);

    assertEquals(expectedResponse, actualResponse);
    verify(mapper, times(1)).requestToModel(updateRequest);
    verify(repository, times(1)).saveAndFlush(studentToUpdate);
    verify(mapper, times(1)).modelToResponse(updatedStudentFromRepo);
  }

  @Test
  @DisplayName("updateStudent: Ошибка при обновлении в репозитории")
  void updateStudent_whenRepositoryUpdateFails_thenThrowException() {
    Long studentIdToUpdate = 5L;
    StudentUpdateRequest updateRequest = new StudentUpdateRequest(studentIdToUpdate, "P-Body", "Core");
    Student studentToUpdate = new Student(studentIdToUpdate, "P-Body", "Core");

    when(mapper.requestToModel(updateRequest)).thenReturn(studentToUpdate);
    when(repository.saveAndFlush(studentToUpdate)).thenThrow(new RuntimeException("Database update error"));

    assertThrows(RuntimeException.class, () -> service.updateStudent(updateRequest));

    verify(mapper, times(1)).requestToModel(updateRequest);
    verify(repository, times(1)).saveAndFlush(studentToUpdate);
    verify(mapper, never()).modelToResponse(any());
  }


  @Test
  @DisplayName("deleteStudent: Успешное удаление существующего студента")
  void deleteStudent_whenStudentExists_thenReturnDeletedStudentResponseAndVerifyDeletion() {
    Long studentIdToDelete = 3L;
    Student studentModel = new Student(studentIdToDelete, "Cave Johnson", "Science");
    StudentResponse expectedResponse = new StudentResponse(studentIdToDelete, "Cave Johnson", "Science");

    when(repository.findById(studentIdToDelete)).thenReturn(Optional.of(studentModel));
    when(mapper.modelToResponse(studentModel)).thenReturn(expectedResponse);

    StudentResponse actualResponse = service.deleteStudent(studentIdToDelete);

    assertEquals(expectedResponse, actualResponse);
    verify(repository, times(1)).findById(studentIdToDelete);
    verify(repository, times(1)).delete(studentModel);
    verify(mapper, times(1)).modelToResponse(studentModel);
  }

  @Test
  @DisplayName("deleteStudent: Студент для удаления не найден, выбрасывается NotFoundException")
  void deleteStudent_whenStudentNotFound_thenThrowNotFoundException() {
    Long studentIdToDelete = 404L;

    when(repository.findById(studentIdToDelete)).thenReturn(Optional.empty());

    NotFoundException exception = assertThrows(NotFoundException.class, () -> service.deleteStudent(studentIdToDelete));

    assertEquals("Студент не найден", exception.getMessage());
    verify(repository, times(1)).findById(studentIdToDelete);
    verify(repository, never()).delete(any());
    verify(mapper, never()).modelToResponse(any());
  }

  @Test
  @DisplayName("deleteStudent: Ошибка при вызове delete в репозитории")
  void deleteStudent_whenRepositoryDeleteFails_thenThrowException() {
    Long studentIdToDelete = 8L;
    Student studentModel = new Student(studentIdToDelete, "Caroline", "Cave");

    when(repository.findById(studentIdToDelete)).thenReturn(Optional.of(studentModel));
    doThrow(new RuntimeException("Deletion failed in DB")).when(repository).delete(studentModel);

    assertThrows(RuntimeException.class, () -> service.deleteStudent(studentIdToDelete));

    verify(repository, times(1)).findById(studentIdToDelete);
    verify(repository, times(1)).delete(studentModel);
    verify(mapper, never()).modelToResponse(any());
  }

}
