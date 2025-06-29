package ru.mai.lessons.rpks.impl;

import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.mai.lessons.rpks.RuleProcessor;
import ru.mai.lessons.rpks.model.Message;
import ru.mai.lessons.rpks.model.Rule;

@Slf4j
public class RulesController implements RuleProcessor {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Message processing(Message message, Rule[] rules) {
        JsonNode node;
        try {
            node = mapper.readTree(message.getValue());
        } catch (Exception e) {
            log.error("Get exception while parsing json", e);
            Message processed = new Message(message);
            processed.setFilterState(false);
            return processed;
        }

        if (rules == null || rules.length == 0) {
            Message processed = new Message(message);
            processed.setFilterState(false);
            return processed;
        }

        for (Rule rule : rules) {
            if (!isRuleValid(rule, node)) {
                Message processed = new Message(message);
                processed.setFilterState(false);
                return processed;
            }
        }

        Message processed = new Message(message);
        processed.setFilterState(true);
        return processed;
    }


    private boolean isRuleValid(Rule rule, JsonNode message) {
        String fieldNameFromRule = rule.getFieldName();
        String valueFromRule = rule.getFilterValue();
        String funcFromRule = rule.getFilterFunctionName();

        JsonNode searchingNode = message.get(fieldNameFromRule);

        if(searchingNode == null) {
            return false;
        }

        String valueFromMessage = searchingNode.asText();

        if (valueFromRule == null || valueFromMessage.isEmpty() ) {
            return false;
        }

        return switch (funcFromRule) {
            case "equals" -> valueFromMessage.equals(valueFromRule);
            case "not_equals" -> !valueFromMessage.equals(valueFromRule);
            case "contains" -> valueFromMessage.contains(valueFromRule);
            case "not_contains" -> !valueFromMessage.contains(valueFromRule);
            default -> throw new UnsupportedOperationException("Wrong rule: " + rule);
        };
    }
}
