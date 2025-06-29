package ru.mai.lessons.rpks.dto.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;
import ru.mai.lessons.rpks.dto.requests.StudentCreateRequest;
import ru.mai.lessons.rpks.dto.requests.StudentUpdateRequest;
import ru.mai.lessons.rpks.dto.respones.StudentResponse;
import ru.mai.lessons.rpks.models.Student;

@Mapper(componentModel = ComponentModel.SPRING)
public interface StudentMapper {

  Student requestToModel(StudentCreateRequest request);

  Student requestToModel(StudentUpdateRequest request);

  StudentResponse modelToResponse(Student student);
}
