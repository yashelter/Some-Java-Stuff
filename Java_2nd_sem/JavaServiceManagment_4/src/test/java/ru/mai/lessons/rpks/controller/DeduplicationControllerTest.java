package ru.mai.lessons.rpks.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.mai.lessons.rpks.model.Deduplication;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class DeduplicationControllerTest extends RulesTest {

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
    void getAllDeduplications() throws Exception {
        var expectedRules = Stream.of(
                        new Deduplication(1, 1, 1, "name", 10L, true),
                        new Deduplication(2, 1, 2, "age", 10L, true),
                        new Deduplication(3, 1, 3, "sex", 10L, true))
                .peek(rule -> createAndCheckDeduplicationRuleInPostgreSQL(rule.getDeduplicationId(), rule.getRuleId(), rule.getFieldName(), rule.getTimeToLiveSec(), rule.isActive()))
                .toList();

        this.mockMvc.perform(get("/deduplication/findAll"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(toJson(expectedRules)));
    }

    @Test
    void getAllDeduplicationsByDeduplicationId() throws Exception {
        var expectedRules = Stream.of(
                        new Deduplication(1, 1, 1, "name", 10L, true),
                        new Deduplication(2, 1, 2, "age", 10L, true),
                        new Deduplication(3, 1, 3, "sex", 10L, true))
                .peek(rule -> createAndCheckDeduplicationRuleInPostgreSQL(rule.getDeduplicationId(), rule.getRuleId(), rule.getFieldName(), rule.getTimeToLiveSec(), rule.isActive()))
                .toList();

        this.mockMvc.perform(get("/deduplication/findAll/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(toJson(expectedRules)));
    }

    @Test
    void getDeduplicationById() throws Exception {
        var expectedRule = Stream.of(
                        new Deduplication(1, 1, 1, "name", 10L, true),
                        new Deduplication(2, 1, 2, "age", 10L, true),
                        new Deduplication(3, 1, 3, "sex", 10L, true))
                .peek(rule -> createAndCheckDeduplicationRuleInPostgreSQL(rule.getDeduplicationId(), rule.getRuleId(), rule.getFieldName(), rule.getTimeToLiveSec(), rule.isActive()))
                .filter(d -> d.getDeduplicationId() == 1 && d.getRuleId() == 2)
                .findFirst().orElse(new Deduplication());

        this.mockMvc.perform(get("/deduplication/find/1/2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(toJson(expectedRule)));
    }

    @Test
    void deleteDeduplication() throws Exception {
        var rules = Stream.of(
                        new Deduplication(1, 1, 1, "name", 10L, true),
                        new Deduplication(2, 1, 2, "age", 10L, true),
                        new Deduplication(3, 1, 3, "sex", 10L, true))
                .peek(rule -> createAndCheckDeduplicationRuleInPostgreSQL(rule.getDeduplicationId(), rule.getRuleId(), rule.getFieldName(), rule.getTimeToLiveSec(), rule.isActive()))
                .toList();

        this.mockMvc.perform(delete("/deduplication/delete"))
                .andDo(print())
                .andExpect(status().isOk());

        rules.forEach(r -> Assertions.assertTrue(getDeduplicationRulesFromDB(r.getDeduplicationId(), r.getRuleId()).isEmpty()));
    }

    @Test
    void deleteDeduplicationById() throws Exception {
        var actualRule = Stream.of(
                        new Deduplication(1, 1, 1, "name", 10L, true),
                        new Deduplication(2, 1, 2, "age", 10L, true),
                        new Deduplication(3, 1, 3, "sex", 10L, true))
                .peek(rule -> createAndCheckDeduplicationRuleInPostgreSQL(rule.getDeduplicationId(), rule.getRuleId(), rule.getFieldName(), rule.getTimeToLiveSec(), rule.isActive()))
                .filter(d -> d.getDeduplicationId() == 1 && d.getRuleId() == 2)
                .findFirst().orElse(new Deduplication());

        this.mockMvc.perform(delete("/deduplication/delete/1/2"))
                .andDo(print())
                .andExpect(status().isOk());

        Assertions.assertTrue(getDeduplicationRulesFromDB(actualRule.getDeduplicationId(), actualRule.getRuleId()).isEmpty());
    }

    @Test
    void save() {
        var expectedRules = Stream.of(
                        new Deduplication(1, 1, 1, "name", 10L, true),
                        new Deduplication(2, 1, 2, "age", 10L, true),
                        new Deduplication(3, 1, 3, "sex", 10L, true))
                .toList();

        expectedRules.forEach(r -> {
            try {
                this.mockMvc.perform(post("/deduplication/save")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(r)))
                        .andDo(print())
                        .andExpect(status().isCreated());
            } catch (Exception e) {
                log.error("Test error", e);
                Assertions.fail();
            }
        });

        expectedRules.forEach(r -> Assertions.assertEquals(r, getDeduplicationRulesFromDB(r.getDeduplicationId(), r.getRuleId()).stream().findFirst()
                .orElse(new Deduplication())));
    }

}