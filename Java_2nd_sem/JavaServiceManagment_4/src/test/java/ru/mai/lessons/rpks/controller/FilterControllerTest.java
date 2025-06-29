package ru.mai.lessons.rpks.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.mai.lessons.rpks.model.Filter;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class FilterControllerTest extends RulesTest {

    @BeforeEach
    void setUp() {
        super.setUp();
        setTableName("filter_rules");
    }

    @AfterEach
    void tearDown() {
        super.tearDown();
    }

    @Test
    void getAllFilters() throws Exception {
        var expectedRules = Stream.of(
                        new Filter(1, 1, 1, "fieldName1", "equals", "value1"),
                        new Filter(2, 1, 2, "fieldName2", "contains", "value2"),
                        new Filter(3, 1, 3, "fieldName3", "not_equals", "value3"))
                .peek(rule -> createAndCheckFilterRuleInPostgreSQL(rule.getFilterId(), rule.getRuleId(), rule.getFieldName(), rule.getFilterFunctionName(), rule.getFilterValue()))
                .toList();

        this.mockMvc.perform(get("/filter/findAll"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(toJson(expectedRules)));
    }

    @Test
    void getAllFiltersByFilterId() throws Exception {
        var expectedRules = Stream.of(
                        new Filter(1, 1, 1, "fieldName1", "equals", "value1"),
                        new Filter(2, 1, 2, "fieldName2", "contains", "value2"),
                        new Filter(3, 1, 3, "fieldName3", "not_equals", "value3"))
                .peek(rule -> createAndCheckFilterRuleInPostgreSQL(rule.getFilterId(), rule.getRuleId(), rule.getFieldName(), rule.getFilterFunctionName(), rule.getFilterValue()))
                .toList();

        this.mockMvc.perform(get("/filter/findAll/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(toJson(expectedRules)));
    }

    @Test
    void getFilterById() throws Exception {
        var expectedRule = Stream.of(
                        new Filter(1, 1, 1, "fieldName1", "equals", "value1"),
                        new Filter(2, 1, 2, "fieldName2", "contains", "value2"),
                        new Filter(3, 1, 3, "fieldName3", "not_equals", "value3"))
                .peek(rule -> createAndCheckFilterRuleInPostgreSQL(rule.getFilterId(), rule.getRuleId(), rule.getFieldName(), rule.getFilterFunctionName(), rule.getFilterValue()))
                .filter(d -> d.getFilterId() == 1 && d.getRuleId() == 2)
                .findFirst().orElse(new Filter());

        this.mockMvc.perform(get("/filter/find/1/2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(toJson(expectedRule)));
    }

    @Test
    void deleteFilter() throws Exception {
        var rules = Stream.of(
                        new Filter(1, 1, 1, "fieldName1", "equals", "value1"),
                        new Filter(2, 1, 2, "fieldName2", "contains", "value2"),
                        new Filter(3, 1, 3, "fieldName3", "not_equals", "value3"))
                .peek(rule -> createAndCheckFilterRuleInPostgreSQL(rule.getFilterId(), rule.getRuleId(), rule.getFieldName(), rule.getFilterFunctionName(), rule.getFilterValue()))
                .toList();

        this.mockMvc.perform(delete("/filter/delete"))
                .andDo(print())
                .andExpect(status().isOk());

        rules.forEach(r -> Assertions.assertTrue(getFilterRulesFromDB(r.getFilterId(), r.getRuleId()).isEmpty()));
    }

    @Test
    void deleteFilterById() throws Exception {
        var actualRule = Stream.of(
                        new Filter(1, 1, 1, "fieldName1", "equals", "value1"),
                        new Filter(2, 1, 2, "fieldName2", "contains", "value2"),
                        new Filter(3, 1, 3, "fieldName3", "not_equals", "value3"))
                .peek(rule -> createAndCheckFilterRuleInPostgreSQL(rule.getFilterId(), rule.getRuleId(), rule.getFieldName(), rule.getFilterFunctionName(), rule.getFilterValue()))
                .filter(d -> d.getFilterId() == 1 && d.getRuleId() == 2)
                .findFirst().orElse(new Filter());

        this.mockMvc.perform(delete("/filter/delete/1/2"))
                .andDo(print())
                .andExpect(status().isOk());

        Assertions.assertTrue(getFilterRulesFromDB(actualRule.getFilterId(), actualRule.getRuleId()).isEmpty());
    }

    @Test
    void save() {
        var expectedRules = Stream.of(
                        new Filter(1, 1, 1, "fieldName1", "equals", "value1"),
                        new Filter(2, 1, 2, "fieldName2", "contains", "value2"),
                        new Filter(3, 1, 3, "fieldName3", "not_equals", "value3"))
                .toList();

        expectedRules.forEach(r -> {
            try {
                this.mockMvc.perform(post("/filter/save")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(r)))
                        .andDo(print())
                        .andExpect(status().isCreated());
            } catch (Exception e) {
                log.error("Test error", e);
                Assertions.fail();
            }
        });

        expectedRules.forEach(r -> Assertions.assertEquals(r, getFilterRulesFromDB(r.getFilterId(), r.getRuleId()).stream().findFirst()
                .orElse(new Filter())));
    }

}