package ru.mai.lessons.rpks.services.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;
import ru.mai.lessons.rpks.dto.mappers.StudentMapper;
import ru.mai.lessons.rpks.dto.requests.StudentCreateRequest;
import ru.mai.lessons.rpks.dto.requests.StudentUpdateRequest;
import ru.mai.lessons.rpks.dto.respones.StudentResponse;
import ru.mai.lessons.rpks.models.Student;
import ru.mai.lessons.rpks.repositories.StudentRepository;
import ru.mai.lessons.rpks.services.StudentService;

@Service
@Transactional
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

  private final StudentRepository repository;
  private final StudentMapper mapper;

  @Override
  public StudentResponse saveStudent(StudentCreateRequest request) {
    Student student = mapper.requestToModel(request);
    Student savedStudent = repository.saveAndFlush(student);
    return mapper.modelToResponse(savedStudent);
  }

  @Override
  public StudentResponse getStudent(Long id) {
    Student student = repository.findById(id).orElse(null);

    if (student != null) {
      return mapper.modelToResponse(student);
    }

    throw new NotFoundException("Студент не найден");
  }

  @Override
  public StudentResponse updateStudent(StudentUpdateRequest request) {
    Student student = mapper.requestToModel(request);
    Student updatedStudent = repository.saveAndFlush(student);
    return mapper.modelToResponse(updatedStudent);
  }

  @Override
  public StudentResponse deleteStudent(Long id) {
    Student student = repository.findById(id).orElse(null);

    if (student != null) {
      repository.delete(student);
      return mapper.modelToResponse(student);
    }

    throw new NotFoundException("Студент не найден");
  }
}
