package ru.mai.lessons.rpks.controllers.impl;

import jakarta.validation.Valid;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.mai.lessons.rpks.controllers.DeduplicationController;
import ru.mai.lessons.rpks.dto.request.DeduplicationRequest;
import ru.mai.lessons.rpks.dto.response.DeduplicationResponse;
import ru.mai.lessons.rpks.services.DeduplicationService;

@Validated
@RestController
@RequestMapping("/deduplication")
@AllArgsConstructor
public class DeduplicationControllerImpl implements DeduplicationController {

  private final DeduplicationService service;
  @Override
  @GetMapping("/findAll")
  @ResponseStatus(value = HttpStatus.OK)
  public Iterable<DeduplicationResponse> getAllDeduplications() {
    return service.getAllDeduplications();
  }

  @Override
  @GetMapping("/findAll/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public Iterable<DeduplicationResponse> getAllDeduplicationsByDeduplicationId(
      @PathVariable("id") long id) {
    return service.getAllDeduplicationsByDeduplicationId(id);
  }

  @Override
  @GetMapping("/find/{deduplicationId}/{ruleId}")
  @ResponseStatus(value = HttpStatus.OK)
  public DeduplicationResponse getDeduplicationById(
      @PathVariable("deduplicationId") long deduplicationId,
      @PathVariable("ruleId") long ruleId) {
    return service.getDeduplicationById(deduplicationId, ruleId);
  }

  @Override
  @DeleteMapping("/delete")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteDeduplication() {
    service.deleteDeduplication();
  }

  @Override
  @DeleteMapping("/delete/{deduplicationId}/{ruleId}")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteDeduplicationById(
      @PathVariable("deduplicationId") long deduplicationId,
      @PathVariable("ruleId") long ruleId) {
    service.deleteDeduplicationById(deduplicationId, ruleId);
  }

  @Override
  @PostMapping("/save")
  @ResponseStatus(value = HttpStatus.CREATED)
  public void save(@RequestBody @Valid DeduplicationRequest deduplication) {
    service.save(deduplication);
  }

}
