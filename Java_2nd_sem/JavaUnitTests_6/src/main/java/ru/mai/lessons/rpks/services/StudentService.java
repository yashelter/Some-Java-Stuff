package ru.mai.lessons.rpks.services;

import ru.mai.lessons.rpks.dto.requests.StudentCreateRequest;
import ru.mai.lessons.rpks.dto.requests.StudentUpdateRequest;
import ru.mai.lessons.rpks.dto.respones.StudentResponse;

public interface StudentService {

  StudentResponse saveStudent(StudentCreateRequest request);

  StudentResponse getStudent(Long id);

  StudentResponse updateStudent(StudentUpdateRequest request);

  StudentResponse deleteStudent(Long id);
}
