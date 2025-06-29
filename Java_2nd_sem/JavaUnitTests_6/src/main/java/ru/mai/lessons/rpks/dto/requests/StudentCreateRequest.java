package ru.mai.lessons.rpks.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(
    name = "StudentCreateRequest",
    description = "Запрос на создание информации о студенте",
    additionalProperties = AdditionalPropertiesValue.FALSE)
public class StudentCreateRequest {

  @Schema(
      name = "fullName",
      description = "Полное имя студента",
      type = "String",
      maxLength = 30
  )
  @NotNull(message = "Полное имя студента не должно быть null")
  @NotBlank(message = "Полное имя студента не должно быть пустым")
  private String fullName;

  @Schema(
      name = "groupName",
      description = "Группа студента",
      type = "String",
      maxLength = 30
  )
  @NotNull(message = "Группа студента не должна быть null")
  @NotBlank(message = "Группа студента не должна быть пустой")
  private String groupName;
}
