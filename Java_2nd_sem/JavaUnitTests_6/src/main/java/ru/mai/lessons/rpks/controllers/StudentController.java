package ru.mai.lessons.rpks.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException.BadRequest;
import org.springframework.web.client.HttpServerErrorException.InternalServerError;
import ru.mai.lessons.rpks.dto.requests.StudentCreateRequest;
import ru.mai.lessons.rpks.dto.requests.StudentUpdateRequest;
import ru.mai.lessons.rpks.dto.respones.StudentResponse;

@Tag(
    name = "Контроллер для управления информацией о студентах",
    description = "Контроллер для управления информацией о студентах"
)
public interface StudentController {

  @Operation(
      summary = "Создание информации о студенте",
      description = "Создание информации о студенте",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное создание информации о студенте",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = StudentResponse.class)
                  )
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Неверный формат запроса",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = BadRequest.class)
              )
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Ошибка на стороне сервиса",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = InternalServerError.class)
              )
          )
      }
  )
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Запрос на создание информации о студенте",
      required = true,
      content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = StudentCreateRequest.class)
      )
  )
  @PostMapping("/save")
  StudentResponse saveStudent(@RequestBody @Valid StudentCreateRequest request);

  @Operation(
      summary = "Получение информации о студенте по его идентификатору",
      description = "Получение информации о студенте по его идентификатору",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное получение информации о студенте",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = StudentResponse.class)
                  )
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Неверный формат запроса",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = BadRequest.class)
              )
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Ошибка на стороне сервиса",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = InternalServerError.class)
              )
          )
      }
  )
  @GetMapping("/get")
  StudentResponse getStudent(@RequestParam("id") @NotNull Long id);

  @Operation(
      summary = "Обновление информации о студенте",
      description = "Обновление информации о студенте",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное обновление информации о студенте",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = StudentResponse.class)
                  )
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Неверный формат запроса",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = BadRequest.class)
              )
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Ошибка на стороне сервиса",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = InternalServerError.class)
              )
          )
      }
  )
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Запрос на обновление информации о студенте",
      required = true,
      content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = StudentUpdateRequest.class)
      )
  )
  @PutMapping("/update")
  StudentResponse updateStudent(@RequestBody @Valid StudentUpdateRequest request);

  @Operation(
      summary = "Удаление информации о студенте по его идентификатору",
      description = "Удаление информации о студенте по его идентификатору",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное удаление информации о студенте",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = StudentResponse.class)
                  )
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Неверный формат запроса",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = BadRequest.class)
              )
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Ошибка на стороне сервиса",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = InternalServerError.class)
              )
          )
      }
  )
  @DeleteMapping("/delete")
  StudentResponse deleteStudent(@RequestParam("id") @NotNull Long id);
}
