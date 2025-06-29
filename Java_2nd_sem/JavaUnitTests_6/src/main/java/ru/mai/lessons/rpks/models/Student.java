package ru.mai.lessons.rpks.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "student")
public class Student {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, unique = true)
//  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "student_id_seq")
//  @SequenceGenerator(name = "student_id_seq", sequenceName = "student_id_seq", allocationSize = 1)
  private Long id;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "group_name", nullable = false)
  private String groupName;
}
