package ru.mai.lessons.rpks.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException.BadRequest;
import org.springframework.web.client.HttpServerErrorException.InternalServerError;
import ru.mai.lessons.rpks.dto.response.TokenResponse;

@Tag(
    name = "Контроллер регистрации",
    description = "Контроллер для регистрации пользователей и выдачи токена"
)
public interface RegisterController {

  @Operation(
      summary = "Регистрация пользователя",
      description = "Выдача токена и запись пользователя в БД",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Успешная регистрация",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = TokenResponse.class)
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
  @GetMapping("/register")
  TokenResponse register(@RequestParam("username") String username);
}
