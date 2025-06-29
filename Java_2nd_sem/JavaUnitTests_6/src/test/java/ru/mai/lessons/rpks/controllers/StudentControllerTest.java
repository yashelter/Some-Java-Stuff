package ru.mai.lessons.rpks.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.dockerjava.api.exception.NotFoundException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.mai.lessons.rpks.controllers.impl.StudentControllerImpl;
import ru.mai.lessons.rpks.dto.requests.StudentCreateRequest;
import ru.mai.lessons.rpks.dto.requests.StudentUpdateRequest;
import ru.mai.lessons.rpks.dto.respones.StudentResponse;
import ru.mai.lessons.rpks.services.StudentService;
import ru.mai.lessons.rpks.utils.JsonUtils;

@AutoConfigureMockMvc
@WebMvcTest(StudentControllerImpl.class)
@TestPropertySource(properties = "server.port=8080")
class StudentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private StudentService service;


  @Test
  @SneakyThrows
  @DisplayName("Успешное создание студента")
  void saveStudent_Success() {
    var req = new StudentCreateRequest("CSharp", "Best");
    var resp = new StudentResponse(10L, "CSharp", "Best");

    when(service.saveStudent(req)).thenReturn(resp);

    mockMvc.perform(post("/student/save")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(req)))
            .andExpect(status().isOk())
            .andExpect(content().json(JsonUtils.toJson(resp)));
  }

  @Test
  @SneakyThrows
  @DisplayName("Ошибка при пустом имени или группе")
  void saveStudent_ValidationError() {
    var badReq = new StudentCreateRequest(null, null);
    mockMvc.perform(post("/student/save")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(badReq)))
            .andExpect(status().is4xxClientError());
  }


  @Test
  @SneakyThrows
  @DisplayName("Успешное получение по ID")
  void getStudent_Success() {
    var resp = new StudentResponse(5L, "CSharp", "Best");
    when(service.getStudent(5L)).thenReturn(resp);

    mockMvc.perform(get("/student/get")
                    .param("id", "5"))
            .andExpect(status().isOk())
            .andExpect(content().json(JsonUtils.toJson(resp)));
  }

  @Test
  @SneakyThrows
  @DisplayName("Ошибка парсинга ID (null)")
  void getStudent_MissingId() {
    mockMvc.perform(get("/student/get"))
            .andExpect(status().is4xxClientError());
  }

  @Test
  @SneakyThrows
  @DisplayName("Студент не найден")
  void getStudent_NotFound() {
    when(service.getStudent(100L))
            .thenThrow(new NotFoundException("not found"));

    mockMvc.perform(get("/student/get")
                    .param("id", "100"))
            .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @SneakyThrows
  @DisplayName("Ошибка сервиса при получении")
  void getStudent_ServiceError() {
    when(service.getStudent(7L)).thenThrow(new RuntimeException(""));

    mockMvc.perform(get("/student/get")
                    .param("id", "7"))
            .andExpect(status().is4xxClientError());
  }


  @Test
  @SneakyThrows
  @DisplayName("Успешное обновление")
  void updateStudent_Success() {
    var req = new StudentUpdateRequest(2L, "CSharp", "Best");
    var resp = new StudentResponse(2L, "CSharp", "Best");
    when(service.updateStudent(req)).thenReturn(resp);

    mockMvc.perform(put("/student/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(req)))
            .andExpect(status().isOk())
            .andExpect(content().json(JsonUtils.toJson(resp)));
  }

  @Test
  @SneakyThrows
  @DisplayName("Ошибка валидации при update")
  void updateStudent_ValidationError() {
    var badReq = new StudentUpdateRequest(null, "", "");
    mockMvc.perform(put("/student/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(badReq)))
            .andExpect(status().is4xxClientError());
  }

  @Test
  @SneakyThrows
  @DisplayName("Студент для обновления не найден")
  void updateStudent_NotFound() {
    var req = new StudentUpdateRequest(99L, "X", "Y");
    when(service.updateStudent(req))
            .thenThrow(new NotFoundException("nope"));

    mockMvc.perform(put("/student/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(req)))
            .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @SneakyThrows
  @DisplayName("Ошибка сервиса при update")
  void updateStudent_ServiceError() {
    var req = new StudentUpdateRequest(3L, "A", "B");
    when(service.updateStudent(req))
            .thenThrow(new RuntimeException("fail"));

    mockMvc.perform(put("/student/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(req)))
            .andExpect(status().is4xxClientError());
  }


  @Test
  @SneakyThrows
  @DisplayName("Успешное удаление")
  void deleteStudent_Success() {
    var resp = new StudentResponse(52L, "Java", "NotBest");
    when(service.deleteStudent(52L)).thenReturn(resp);

    mockMvc.perform(delete("/student/delete")
                    .param("id", "52"))
            .andExpect(status().isOk())
            .andExpect(content().json(JsonUtils.toJson(resp)));
  }

  @Test
  @SneakyThrows
  @DisplayName("Ошибка при отсутствии параметра id")
  void deleteStudent_MissingId() {
    mockMvc.perform(delete("/student/delete"))
            .andExpect(status().is4xxClientError());
  }

  @Test
  @SneakyThrows
  @DisplayName("Удаляемый студент не найден")
  void deleteStudent_NotFound() {
    when(service.deleteStudent(50L)).thenThrow(new NotFoundException(""));

    mockMvc.perform(delete("/student/delete").param("id", "50"))
            .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @SneakyThrows
  @DisplayName("Ошибка сервиса при delete")
  void deleteStudent_ServiceError() {
    when(service.deleteStudent(6L)).thenThrow(new RuntimeException("error"));

    mockMvc.perform(
            delete("/student/delete").param("id", "6"))
            .andExpect(status().is4xxClientError());
  }

}
