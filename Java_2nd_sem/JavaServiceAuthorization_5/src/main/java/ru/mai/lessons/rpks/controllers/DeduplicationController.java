package ru.mai.lessons.rpks.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpClientErrorException.BadRequest;
import org.springframework.web.client.HttpServerErrorException.InternalServerError;
import ru.mai.lessons.rpks.dto.request.DeduplicationRequest;
import ru.mai.lessons.rpks.dto.response.DeduplicationResponse;

@Tag(
    name = "Контроллер дедубликации",
    description = "Контроллер для операций по дедубликации данных"
)
public interface DeduplicationController {

  @Operation(
      summary = "Получить информацию о всех правилах дедубликации в БД",
      description = "Получить информацию о всех правилах дедубликации в БД",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное получение информации",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = DeduplicationResponse.class)
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
  @GetMapping("/findAll")
  Iterable<DeduplicationResponse> getAllDeduplications();

  @Operation(
      summary = "Получить информацию о всех правилах дедубликации в БД по deduplication id",
      description = "Получить информацию о всех правилах дедубликации в БД по deduplication id",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное получение информации",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = DeduplicationResponse.class)
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
  @GetMapping("/findAll/{id}")
  Iterable<DeduplicationResponse> getAllDeduplicationsByDeduplicationId(
      @PathVariable("id") long id);

  @Operation(
      summary = "Получить информацию о правиле дедубликации по deduplication id и rule id",
      description = "Получить информацию о правиле дедубликации по deduplication id и rule id",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное получение информации",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = DeduplicationResponse.class)
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
  @GetMapping("/find/{deduplicationId}/{ruleId}")
  DeduplicationResponse getDeduplicationById(
      @PathVariable("deduplicationId") long deduplicationId,
      @PathVariable("ruleId") long ruleId);

  @Operation(
      summary = "Удалить информацию о всех правилах дедубликации",
      description = "Удалить информацию о всех правилах дедубликации",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное удаление записей"
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
  void deleteDeduplication();

  @Operation(
      summary = "Удалить информацию по конкретному правилу дедубликации с deduplication id и rule id",
      description = "Удалить информацию по конкретному правилу дедубликации с deduplication id и rule id",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное удаление записей"
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
  @DeleteMapping("/delete/{deduplicationId}/{ruleId}")
  void deleteDeduplicationById(
      @PathVariable("deduplicationId") long deduplicationId,
      @PathVariable("ruleId") long ruleId);

  @Operation(
      summary = "Создать правило дедубликации",
      description = "Создать правило дедубликации",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное создание правила"
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
      description = "Запрос на создание правила дедубликации",
      required = true,
      content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = DeduplicationRequest.class)
      )
  )
  @PostMapping("/save")
  void save(@RequestBody @Valid DeduplicationRequest deduplication);
}
