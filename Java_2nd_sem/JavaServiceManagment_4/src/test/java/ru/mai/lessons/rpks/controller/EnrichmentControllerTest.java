package ru.mai.lessons.rpks.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.mai.lessons.rpks.model.Enrichment;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class EnrichmentControllerTest extends RulesTest {

    @BeforeEach
    void setUp() {
        super.setUp();
        setTableName("enrichment_rules");
    }

    @AfterEach
    void tearDown() {
        super.tearDown();
    }

    @Test
    void getAllEnrichment() throws Exception {
        var expectedRules = Stream.of(
                        new Enrichment(1, 1, 1, "field1", "fieldEnrichment1", "value1", "valueDefault1"),
                        new Enrichment(2, 1, 2, "field2", "fieldEnrichment2", "value2", "valueDefault2"),
                        new Enrichment(3, 1, 3, "field3", "fieldEnrichment3", "value3", "valueDefault3"))
                .peek(rule -> createAndCheckEnrichmentRuleInPostgreSQL(rule.getEnrichmentId(), rule.getRuleId(), rule.getFieldName(), rule.getFieldNameEnrichment(), rule.getFieldValue(), rule.getFieldValueDefault()))
                .toList();

        this.mockMvc.perform(get("/enrichment/findAll"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(toJson(expectedRules)));
    }

    @Test
    void getAllEnrichmentsByEnrichmentId() throws Exception {
        var expectedRules = Stream.of(
                        new Enrichment(1, 1, 1, "field1", "fieldEnrichment1", "value1", "valueDefault1"),
                        new Enrichment(2, 1, 2, "field2", "fieldEnrichment2", "value2", "valueDefault2"),
                        new Enrichment(3, 1, 3, "field3", "fieldEnrichment3", "value3", "valueDefault3"))
                .peek(rule -> createAndCheckEnrichmentRuleInPostgreSQL(rule.getEnrichmentId(), rule.getRuleId(), rule.getFieldName(), rule.getFieldNameEnrichment(), rule.getFieldValue(), rule.getFieldValueDefault()))
                .toList();

        this.mockMvc.perform(get("/enrichment/findAll/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(toJson(expectedRules)));
    }

    @Test
    void getEnrichmentById() throws Exception {
        var expectedRule = Stream.of(
                        new Enrichment(1, 1, 1, "field1", "fieldEnrichment1", "value1", "valueDefault1"),
                        new Enrichment(2, 1, 2, "field2", "fieldEnrichment2", "value2", "valueDefault2"),
                        new Enrichment(3, 1, 3, "field3", "fieldEnrichment3", "value3", "valueDefault3"))
                .peek(rule -> createAndCheckEnrichmentRuleInPostgreSQL(rule.getEnrichmentId(), rule.getRuleId(), rule.getFieldName(), rule.getFieldNameEnrichment(), rule.getFieldValue(), rule.getFieldValueDefault()))
                .filter(d -> d.getEnrichmentId() == 1 && d.getRuleId() == 2)
                .findFirst().orElse(new Enrichment());

        this.mockMvc.perform(get("/enrichment/find/1/2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(toJson(expectedRule)));
    }

    @Test
    void deleteEnrichment() throws Exception {
        var rules = Stream.of(
                        new Enrichment(1, 1, 1, "field1", "fieldEnrichment1", "value1", "valueDefault1"),
                        new Enrichment(2, 1, 2, "field2", "fieldEnrichment2", "value2", "valueDefault2"),
                        new Enrichment(3, 1, 3, "field3", "fieldEnrichment3", "value3", "valueDefault3"))
                .peek(rule -> createAndCheckEnrichmentRuleInPostgreSQL(rule.getEnrichmentId(), rule.getRuleId(), rule.getFieldName(), rule.getFieldNameEnrichment(), rule.getFieldValue(), rule.getFieldValueDefault()))
                .toList();

        this.mockMvc.perform(delete("/enrichment/delete"))
                .andDo(print())
                .andExpect(status().isOk());

        rules.forEach(r -> Assertions.assertTrue(getEnrichmentRulesFromDB(r.getEnrichmentId(), r.getRuleId()).isEmpty()));
    }

    @Test
    void deleteEnrichmentById() throws Exception {
        var actualRule = Stream.of(
                        new Enrichment(1, 1, 1, "field1", "fieldEnrichment1", "value1", "valueDefault1"),
                        new Enrichment(2, 1, 2, "field2", "fieldEnrichment2", "value2", "valueDefault2"),
                        new Enrichment(3, 1, 3, "field3", "fieldEnrichment3", "value3", "valueDefault3"))
                .peek(rule -> createAndCheckEnrichmentRuleInPostgreSQL(rule.getEnrichmentId(), rule.getRuleId(), rule.getFieldName(), rule.getFieldNameEnrichment(), rule.getFieldValue(), rule.getFieldValueDefault()))
                .filter(d -> d.getEnrichmentId() == 1 && d.getRuleId() == 2)
                .findFirst().orElse(new Enrichment());

        this.mockMvc.perform(delete("/enrichment/delete/1/2"))
                .andDo(print())
                .andExpect(status().isOk());

        Assertions.assertTrue(getEnrichmentRulesFromDB(actualRule.getEnrichmentId(), actualRule.getRuleId()).isEmpty());
    }

    @Test
    void save() {
        var expectedRules = Stream.of(
                        new Enrichment(1, 1, 1, "field1", "fieldEnrichment1", "value1", "valueDefault1"),
                        new Enrichment(2, 1, 2, "field2", "fieldEnrichment2", "value2", "valueDefault2"),
                        new Enrichment(3, 1, 3, "field3", "fieldEnrichment3", "value3", "valueDefault3"))
                .toList();

        expectedRules.forEach(r -> {
            try {
                this.mockMvc.perform(post("/enrichment/save")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(r)))
                        .andDo(print())
                        .andExpect(status().isCreated());
            } catch (Exception e) {
                log.error("Test error", e);
                Assertions.fail();
            }
        });

        expectedRules.forEach(r -> Assertions.assertEquals(r, getEnrichmentRulesFromDB(r.getEnrichmentId(), r.getRuleId()).stream().findFirst()
                .orElse(new Enrichment())));
    }

}