package ru.mai.lessons.rpks.clients;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.mai.lessons.rpks.dto.request.DeduplicationRequest;
import ru.mai.lessons.rpks.dto.response.DeduplicationResponse;

/**
 * Клиент для контролера дедубликации
 */
@FeignClient(
    name = "deduplicationClient",
    url = "${feign.client.url.deduplication}"
)


public interface DeduplicationClient {
    @GetMapping("/findAll")
    @Operation(summary = "Получить информацию о всех правилах дедубликации в БД")
    public Iterable<DeduplicationResponse> getAllDeduplications();

    @GetMapping("/findAll/{id}")
    @Operation(summary = "Получить информацию о всех правилах дедубликации в БД по deduplication id")
    public Iterable<DeduplicationResponse> getAllDeduplicationsByDeduplicationId(@PathVariable long id);

    @GetMapping("/find/{deduplicationId}/{ruleId}")
    @Operation(summary = "Получить информацию о правиле дедубликации по deduplication id и rule id")
    public DeduplicationResponse getDeduplicationById(@PathVariable long deduplicationId, @PathVariable long ruleId);

    @DeleteMapping("/delete")
    @Operation(summary = "Удалить информацию о всех правилах дедубликации")
    public void deleteDeduplication();

    @Transactional
    @DeleteMapping("/delete/{deduplicationId}/{ruleId}")
    @Operation(summary = "Удалить информацию по конкретному правилу дедубликации с deduplication id и rule id")
    public void deleteDeduplicationById(@PathVariable long deduplicationId, @PathVariable long ruleId);

    @PostMapping("/save")
    @ResponseStatus(value = HttpStatus.CREATED)
    @Operation(summary = "Создать правило дедубликации")
    public void save(@RequestBody @Valid DeduplicationRequest deduplication) ;

}
