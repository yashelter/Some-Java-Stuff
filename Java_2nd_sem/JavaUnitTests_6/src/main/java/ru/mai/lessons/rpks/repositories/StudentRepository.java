package ru.mai.lessons.rpks.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mai.lessons.rpks.models.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

}
