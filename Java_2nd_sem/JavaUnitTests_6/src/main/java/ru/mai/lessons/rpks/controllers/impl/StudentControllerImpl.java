package ru.mai.lessons.rpks.controllers.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.mai.lessons.rpks.controllers.StudentController;
import ru.mai.lessons.rpks.dto.requests.StudentCreateRequest;
import ru.mai.lessons.rpks.dto.requests.StudentUpdateRequest;
import ru.mai.lessons.rpks.dto.respones.StudentResponse;
import ru.mai.lessons.rpks.services.StudentService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/student")
public class StudentControllerImpl implements StudentController {

  private final StudentService service;

  @Override
  @PostMapping("/save")
  @ResponseStatus(HttpStatus.OK)
  public StudentResponse saveStudent(StudentCreateRequest request) {
    log.info("Запрос на создание информации о студенте: request={}", request);
    StudentResponse response = service.saveStudent(request);
    log.info("Ответ о создании информации о студенте: response={}", response);
    return response;
  }

  @Override
  @GetMapping("/get")
  @ResponseStatus(HttpStatus.OK)
  public StudentResponse getStudent(Long id) {
    log.info("Запрос на получение информации о студенте: id={}", id);
    StudentResponse response = service.getStudent(id);
    log.info("Ответ о получении информации о студенте: response={}", response);
    return response;
  }

  @Override
  @PutMapping("/update")
  @ResponseStatus(HttpStatus.OK)
  public StudentResponse updateStudent(StudentUpdateRequest request) {
    log.info("Запрос на обновление информации о студенте: request={}", request);
    StudentResponse response = service.updateStudent(request);
    log.info("Ответ о обновлении информации о студенте: response={}", response);
    return response;
  }

  @Override
  @DeleteMapping("/delete")
  @ResponseStatus(HttpStatus.OK)
  public StudentResponse deleteStudent(Long id) {
    log.info("Запрос на удаление информации о студенте: id={}", id);
    StudentResponse response = service.deleteStudent(id);
    log.info("Ответ о удалении информации о студенте: response={}", response);
    return response;
  }
}
