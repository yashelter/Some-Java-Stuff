package ru.mai.lessons.rpks.clients;

import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mai.lessons.rpks.dto.request.EnrichmentRequest;
import ru.mai.lessons.rpks.dto.response.EnrichmentResponse;

/**
 * Клиент для контролера обогащения
 */
@FeignClient(
    name = "enrichmentClient",
    url = "${feign.client.url.enrichment}"
)


public interface EnrichmentClient {
    @GetMapping("/findAll")
    @Operation(summary = "Получить информацию о всех правилах обогащения в БД")
    public Iterable<EnrichmentResponse> getAllEnrichments();
    @GetMapping("/findAll/{id}")
    @Operation(summary = "Получить информацию о всех правилах обогащения в БД по enrichment id")
    public Iterable<EnrichmentResponse> getAllEnrichmentsByEnrichmentId(@PathVariable long id);

    @GetMapping("/find/{enrichmentId}/{ruleId}")
    @Operation(summary = "Получить информацию о правиле обогащения по enrichment id и rule id")
    public EnrichmentResponse getEnrichmentById(@PathVariable long enrichmentId, @PathVariable long ruleId);

    @DeleteMapping("/delete")
    @Operation(summary = "Удалить информацию о всех правилах обогащения")
    public void deleteEnrichment() ;

    @DeleteMapping("/delete/{enrichmentId}/{ruleId}")
    @Operation(summary = "Удалить информацию по конкретному правилу обогащения с enrichment id и rule id")
    public void deleteEnrichmentById(@PathVariable long enrichmentId, @PathVariable long ruleId) ;

    @PostMapping("/save")
    @Operation(summary = "Создать правило обогащения")
    @ResponseStatus(value = HttpStatus.CREATED)
    public void save(@RequestBody @Valid EnrichmentRequest enrichment) ;
}
