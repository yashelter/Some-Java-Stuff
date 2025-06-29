package ru.mai.lessons.rpks.clients;

import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mai.lessons.rpks.dto.request.FilterRequest;
import ru.mai.lessons.rpks.dto.response.FilterResponse;

/**
 * Клиент для контролера фильтрации
 */
@FeignClient(
    name = "filterClient",
    url = "${feign.client.url.filter}"
)


public interface FilterClient {
    @GetMapping("/findAll")
    @Operation(summary = "Получить информацию о всех фильтрах в БД")
    public Iterable<FilterResponse> getAllFilters();
    @GetMapping("/findAll/{id}")
    @Operation(summary = "Получить информацию о всех фильтрах в БД по filter id")
    public Iterable<FilterResponse> getAllFiltersByFilterId(@PathVariable long id);
    @GetMapping("/find/{filterId}/{ruleId}")
    @Operation(summary = "Получить информацию о фильтре по filter id и rule id")
    public FilterResponse getFilterByFilterIdAndRuleId(@PathVariable long filterId, @PathVariable long ruleId);
    @DeleteMapping("/delete")
    @Operation(summary = "Удалить информацию о всех фильтрах")
    public void deleteFilter();

    @DeleteMapping("/delete/{filterId}/{ruleId}")
    @Operation(summary = "Удалить информацию по конкретному фильтру filter id и rule id")
    public void deleteFilterById(@PathVariable long filterId, @PathVariable long ruleId);
    @PostMapping("/save")
    @Operation(summary = "Создать фильтр")
    @ResponseStatus(value = HttpStatus.CREATED)
    public void save(@RequestBody @Valid FilterRequest filter);
}
