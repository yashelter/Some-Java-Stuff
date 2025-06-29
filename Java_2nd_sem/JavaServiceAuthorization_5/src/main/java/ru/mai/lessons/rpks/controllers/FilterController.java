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
import ru.mai.lessons.rpks.dto.request.FilterRequest;
import ru.mai.lessons.rpks.dto.response.FilterResponse;

@Tag(
    name = "Контроллер фильтрации",
    description = "Контроллер для операций по фильтрации данных"
)
public interface FilterController {

  @Operation(
      summary = "Получить информацию о всех фильтрах в БД",
      description = "Получить информацию о всех фильтрах в БД",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное получение информации",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = FilterResponse.class)
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
  Iterable<FilterResponse> getAllFilters();

  @Operation(
      summary = "Получить информацию о всех фильтрах в БД по filter id",
      description = "Получить информацию о всех фильтрах в БД по filter id",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное получение информации",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = FilterResponse.class)
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
  Iterable<FilterResponse> getAllFiltersByFilterId(
      @PathVariable("id") long id);

  @Operation(
      summary = "Получить информацию о фильтре по filter id и rule id",
      description = "Получить информацию о фильтре по filter id и rule id",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное получение информации",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = FilterResponse.class)
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
  @GetMapping("/find/{filterId}/{ruleId}")
  FilterResponse getFilterByFilterIdAndRuleId(
      @PathVariable("filterId") long filterId,
      @PathVariable("ruleId") long ruleId);

  @Operation(
      summary = "Удалить информацию о всех фильтрах",
      description = "Удалить информацию о всех фильтрах",
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
  void deleteFilter();

  @Operation(
      summary = "Удалить информацию по конкретному фильтру filter id и rule id",
      description = "Удалить информацию по конкретному фильтру filter id и rule id",
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
  @DeleteMapping("/delete/{filterId}/{ruleId}")
  void deleteFilterById(
      @PathVariable("filterId") long filterId,
      @PathVariable("ruleId") long ruleId);

  @Operation(
      summary = "Создать фильтр",
      description = "Создать фильтр",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешное создание фильтра"
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
      description = "Запрос на создание фильтра",
      required = true,
      content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = FilterRequest.class)
      )
  )
  @PostMapping("/save")
  void save(@RequestBody @Valid FilterRequest filter);
}
