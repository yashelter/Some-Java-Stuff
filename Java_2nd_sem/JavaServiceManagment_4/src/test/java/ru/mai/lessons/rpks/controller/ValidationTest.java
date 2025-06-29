package ru.mai.lessons.rpks.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.mai.lessons.rpks.model.Deduplication;
import ru.mai.lessons.rpks.model.Enrichment;
import ru.mai.lessons.rpks.model.Filter;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class ValidationTest extends RulesTest {

    @BeforeEach
    void setUp() {
        super.setUp();
        setTableName("deduplication_rules");
    }

    @AfterEach
    void tearDown() {
        super.tearDown();
    }

    @Test
    void deduplicationSave() {
        var expectedRules = Stream.of(
                        new Deduplication(0, 0, 0, "name", 10L, true),
                        new Deduplication(0, 0, 1, "age", 10L, true),
                        new Deduplication(0, 1, 0, "sex", 10L, true),
                        new Deduplication(0, 1, 1, "", 10L, true))
                .toList();

        expectedRules.forEach(r -> {
            try {
                this.mockMvc.perform(post("/deduplication/save")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(r)))
                        .andDo(print())
                        .andExpect(status().isBadRequest());
            } catch (Exception e) {
                log.error("Test error", e);
                Assertions.fail();
            }
        });
    }

    @Test
    void enrichmentSave() {
        var expectedRules = Stream.of(
                        new Enrichment(0, 0, 0, "field1", "fieldEnrichment1", "value1", "valueDefault1"),
                        new Enrichment(0, 0, 1, "field2", "fieldEnrichment2", "value2", "valueDefault2"),
                        new Enrichment(0, 1, 0, "field3", "fieldEnrichment3", "value3", "valueDefault3"),
                        new Enrichment(0, 1, 1, "", "fieldEnrichment4", "value4", "valueDefault4"),
                        new Enrichment(0, 1, 1, "field5", "", "value5", "valueDefault5"))
                .toList();

        expectedRules.forEach(r -> {
            try {
                this.mockMvc.perform(post("/enrichment/save")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(r)))
                        .andDo(print())
                        .andExpect(status().isBadRequest());
            } catch (Exception e) {
                log.error("Test error", e);
                Assertions.fail();
            }
        });
    }

    @Test
    void filterSave() {
        var expectedRules = Stream.of(
                        new Filter(0, 0, 0, "fieldName1", "equals", "value1"),
                        new Filter(0, 0, 1, "fieldName2", "contains", "value2"),
                        new Filter(0, 1, 0, "fieldName3", "not_equals", "value3"),
                        new Filter(0, 1, 1, "", "not_equals", "value4"),
                        new Filter(0, 1, 1, "fieldName5", "", "value5"),
                        new Filter(0, 1, 1, "fieldName6", "test", "value6"))
                .toList();

        expectedRules.forEach(r -> {
            try {
                this.mockMvc.perform(post("/filter/save")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(r)))
                        .andDo(print())
                        .andExpect(status().isBadRequest());
            } catch (Exception e) {
                log.error("Test error", e);
                Assertions.fail();
            }
        });
    }

}