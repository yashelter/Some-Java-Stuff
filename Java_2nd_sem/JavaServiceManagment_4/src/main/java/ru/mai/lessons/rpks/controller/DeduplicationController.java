package ru.mai.lessons.rpks.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mai.lessons.rpks.model.Deduplication;
import ru.mai.lessons.rpks.services.DeduplicationService;

@RestController
@RequestMapping("deduplication")
@AllArgsConstructor
public class DeduplicationController {

    private final DeduplicationService service;

    @GetMapping("/findAll")
    @Operation(summary = "Получить информацию о всех правилах дедубликации в БД")
    public Iterable<Deduplication> getAllDeduplications() {
        return service.getAllDeduplications();
    }

    @GetMapping("/findAll/{id}")
    @Operation(summary = "Получить информацию о всех правилах дедубликации в БД по deduplication id")
    public Iterable<Deduplication> getAllDeduplicationsByDeduplicationId(@PathVariable long id) {
        return service.getAllDeduplicationsByDeduplicationId(id);
    }

    @GetMapping("/find/{deduplicationId}/{ruleId}")
    @Operation(summary = "Получить информацию о правиле дедубликации по deduplication id и rule id")
    public Deduplication getDeduplicationById(@PathVariable long deduplicationId, @PathVariable long ruleId) {
        return service.getDeduplicationById(deduplicationId, ruleId);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Удалить информацию о всех правилах дедубликации")
    public void deleteDeduplication() {
        service.deleteDeduplication();
    }

    @Transactional
    @DeleteMapping("/delete/{deduplicationId}/{ruleId}")
    @Operation(summary = "Удалить информацию по конкретному правилу дедубликации с deduplication id и rule id")
    public void deleteDeduplicationById(@PathVariable long deduplicationId, @PathVariable long ruleId) {
        service.deleteDeduplicationById(deduplicationId, ruleId);
    }

    @PostMapping("/save")
    @ResponseStatus(value = HttpStatus.CREATED)
    @Operation(summary = "Создать правило дедубликации")
    public void save(@RequestBody @Valid Deduplication deduplication) {
        service.save(deduplication);
    }

}
