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
import ru.mai.lessons.rpks.dto.request.EnrichmentRequest;
import ru.mai.lessons.rpks.dto.response.EnrichmentResponse;

@Tag(
    name = "Контроллер обогащения",
    description = "Контроллер для операций по обогащению данных"
)
public interface EnrichmentController {

  @Operation(
      summary = "Получить информацию о всех правилах обогащения в БД",
      description = "Получить информацию о всех правилах обогащения в БД",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное получение информации",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = EnrichmentResponse.class)
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
  Iterable<EnrichmentResponse> getAllEnrichmentRequests();

  @Operation(
      summary = "Получить информацию о всех правилах обогащения в БД по enrichment id",
      description = "Получить информацию о всех правилах обогащения в БД по enrichment id",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное получение информации",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = EnrichmentResponse.class)
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
  Iterable<EnrichmentResponse> getAllEnrichmentRequestsByEnrichmentRequestId(
      @PathVariable("id") long id);

  @Operation(
      summary = "Получить информацию о правиле обогащения по enrichment id и rule id",
      description = "Получить информацию о правиле обогащения по enrichment id и rule id",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное получение информации",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = EnrichmentResponse.class)
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
  @GetMapping("/find/{enrichmentId}/{ruleId}")
  EnrichmentResponse getEnrichmentRequestById(
      @PathVariable("enrichmentId") long enrichmentId,
      @PathVariable("ruleId") long ruleId);

  @Operation(
      summary = "Удалить информацию о всех правилах обогащения",
      description = "Удалить информацию о всех правилах обогащения",
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
  void deleteEnrichmentRequest();

  @Operation(
      summary = "Удалить информацию по конкретному правилу обогащения с enrichment id и rule id",
      description = "Удалить информацию по конкретному правилу обогащения с enrichment id и rule id",
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
  @DeleteMapping("/delete/{enrichmentId}/{ruleId}")
  void deleteEnrichmentRequestById(
      @PathVariable("enrichmentId") long enrichmentId,
      @PathVariable("ruleId") long ruleId);

  @Operation(
      summary = "Создать правило обогащения",
      description = "Создать правило обогащения",
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
      description = "Запрос на создание правила обогащения",
      required = true,
      content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = EnrichmentRequest.class)
      )
  )
  @PostMapping("/save")
  void save(@RequestBody @Valid EnrichmentRequest enrichment);
}
