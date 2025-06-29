package ru.mai.lessons.rpks.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.MongoDBClientEnricher;
import ru.mai.lessons.rpks.RuleProcessor;
import ru.mai.lessons.rpks.model.Message;
import ru.mai.lessons.rpks.model.Rule;

import java.util.*;


@Slf4j
public class RulesController implements RuleProcessor, AutoCloseable {
    private final MongoDBClientEnricher mongo;

    public RulesController(Config config) {
        mongo = new MongoDBController(config);
    }

    @Override
    public Message processing(Message message, Rule[] rules) {
        if (message == null || message.getValue().isEmpty()) {
            log.warn("Message is null or empty");
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode messageInJson;

        try {
            messageInJson = objectMapper.readTree(message.getValue());
        } catch (JsonProcessingException e) {
            log.error("invalid json format");
            return null;
        }

        if (rules == null || rules.length == 0) {
            log.warn("Rules is null or empty");
            return message;
        }

        Rule[] mapFieldRuleId = getActiveRulesForFields(rules).values().toArray(new Rule[0]);

        log.info(messageInJson.toString());
        mongo.enrichMessage(messageInJson, mapFieldRuleId);
        return new Message(messageInJson.toString());

    }

    private Map<String, Rule> getActiveRulesForFields(Rule[] rules) {
        Map<String, Rule> activeRules = new TreeMap<>();
        for (Rule currentRule : rules) {
            String fieldName = currentRule.getFieldName();
            Long currentId = currentRule.getRuleId();
            var entry = activeRules.get(fieldName);

            if (entry == null || entry.getRuleId() < currentId) {
                activeRules.put(fieldName, currentRule);
            }
        }
        return activeRules;
    }


    @Override
    public void close() {
        mongo.close();
    }

}