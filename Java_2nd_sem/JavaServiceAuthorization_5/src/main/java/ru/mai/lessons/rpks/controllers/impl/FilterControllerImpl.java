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
import ru.mai.lessons.rpks.controllers.FilterController;
import ru.mai.lessons.rpks.dto.request.FilterRequest;
import ru.mai.lessons.rpks.dto.response.FilterResponse;
import ru.mai.lessons.rpks.services.FilterService;

@Validated
@RestController
@RequestMapping("/filter")
@AllArgsConstructor
public class FilterControllerImpl implements FilterController {

  private final FilterService service;

  @Override
  @GetMapping("/findAll")
  @ResponseStatus(value = HttpStatus.OK)
  public Iterable<FilterResponse> getAllFilters() {
    return service.getAllFilters();
  }

  @Override
  @GetMapping("/findAll/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public Iterable<FilterResponse> getAllFiltersByFilterId(
      @PathVariable("id") long id) {
    return service.getAllFiltersByFilterId(id);
  }

  @Override
  @GetMapping("/find/{filterId}/{ruleId}")
  @ResponseStatus(value = HttpStatus.OK)
  public FilterResponse getFilterByFilterIdAndRuleId(
      @PathVariable("filterId") long filterId,
      @PathVariable("ruleId") long ruleId) {
   return service.getFilterByFilterIdAndRuleId(filterId, ruleId);
  }

  @Override
  @DeleteMapping("/delete")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteFilter() {
    service.deleteFilter();
  }

  @Override
  @DeleteMapping("/delete/{filterId}/{ruleId}")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteFilterById(
      @PathVariable("filterId") long filterId,
      @PathVariable("ruleId") long ruleId) {
    service.deleteFilterById(filterId, ruleId);
  }

  @Override
  @PostMapping("/save")
  @ResponseStatus(value = HttpStatus.CREATED)
  public void save(@RequestBody @Valid FilterRequest filter) {
    service.save(filter);
  }
}
