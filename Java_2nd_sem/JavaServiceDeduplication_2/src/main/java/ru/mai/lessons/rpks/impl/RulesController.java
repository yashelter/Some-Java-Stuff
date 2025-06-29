package ru.mai.lessons.rpks.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.RedisClient;
import ru.mai.lessons.rpks.RuleProcessor;
import ru.mai.lessons.rpks.model.Message;
import ru.mai.lessons.rpks.model.Rule;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class RulesController implements RuleProcessor, AutoCloseable {

    private static final String KEY_SEPARATOR = ":";
    private static final String NULL_FIELD_PLACEHOLDER = "<null>";

    private final RedisClient redisClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private record DeduplicationInfo(String key, long lifetime) {}


    public RulesController(Config config) {
        this.redisClient = new RedisClientController(config);
        log.info("ImprovedRulesController initialized.");
    }

    @Override
    public Message processing(Message message, Rule[] rules) {
        if (!isValidMessage(message)) {
            return message;
        }

        List<Rule> activeRules = filterActiveRules(rules);
        if (activeRules.isEmpty()) {
            log.info("No active deduplication rules found or no rules provided. Allowing message.");
            message.setDeduplicationState(true);
            return message;
        }

        Optional<JsonNode> jsonNodeOptional = parseMessageValue(message);
        if (jsonNodeOptional.isEmpty()) {
            return message;
        }
        JsonNode jsonNode = jsonNodeOptional.get();

        Optional<DeduplicationInfo> deduplicationInfoOptional = buildDeduplicationInfo(activeRules, jsonNode);
        if (deduplicationInfoOptional.isEmpty()) {
            log.warn("Could not build valid deduplication key/lifetime. Allowing message.");
            message.setDeduplicationState(true);
            return message;
        }
        DeduplicationInfo deduplicationInfo = deduplicationInfoOptional.get();

        boolean successfullySet = checkAndSetRedisKey(deduplicationInfo);
        message.setDeduplicationState(successfullySet);

        return message;
    }


    private boolean isValidMessage(Message message) {
        if (message == null || message.getValue() == null || message.getValue().isEmpty()) {
            log.warn("Invalid message received (null or empty value). Blocking.");
            if (message != null) message.setDeduplicationState(false);
            return false;
        }
        return true;
    }

    private boolean isValidRule(Rule rule) {
        if (rule.getFieldName() == null || rule.getFieldName().trim().isEmpty()) {
            log.warn("Rule not valid: {}", rule);
            return false;
        }
        return true;
    }

    private List<Rule> filterActiveRules(Rule[] rules) {
        if (rules == null || rules.length == 0) {
            return List.of();
        }
        return Arrays.stream(rules)
                .filter(Objects::nonNull)
                .filter(rule -> Boolean.TRUE.equals(rule.getIsActive()))
                .toList();
    }



    private Optional<JsonNode> parseMessageValue(Message message) {
        try {
            return Optional.of(objectMapper.readTree(message.getValue()));
        } catch (JsonProcessingException e) {
            log.error("Failed to parse message value to JSON: {}. Blocking message.", e.getMessage());
            message.setDeduplicationState(false);
            return Optional.empty();
        }
    }

    private Optional<DeduplicationInfo> buildDeduplicationInfo(List<Rule> activeRules, JsonNode jsonNode) {
        StringBuilder keyBuilder = new StringBuilder();
        long minLifetime = Long.MAX_VALUE;

        for (Rule rule : activeRules) {
            String fieldName = rule.getFieldName();
            if (!isValidRule(rule)) {
                continue;
            }

            JsonNode fieldNode = jsonNode.get(fieldName);
            String fieldValue = (fieldNode.isNull()) ? NULL_FIELD_PLACEHOLDER : fieldNode.asText();

            if (!keyBuilder.isEmpty()) {
                keyBuilder.append(KEY_SEPARATOR);
            }
            keyBuilder.append(fieldValue);

            long currentTTL = rule.getTimeToLiveSec() != null ? rule.getTimeToLiveSec() : 0L;
            if (currentTTL > 0 && currentTTL < minLifetime) {
                minLifetime = currentTTL;
            }
        }

        String deduplicationKey = keyBuilder.toString();

        if (deduplicationKey.isEmpty()) {
            log.warn("Generated deduplication key is empty (likely due to bad rule configuration).");
            return Optional.empty();
        }

        if (minLifetime == Long.MAX_VALUE) {
            log.warn("No valid positive lifetime found among active rules for key [{}].", deduplicationKey);
            return Optional.empty();
        }

        return Optional.of(new DeduplicationInfo(deduplicationKey, minLifetime));
    }

    private boolean checkAndSetRedisKey(DeduplicationInfo info) {
        log.debug("Attempting to set key in Redis: '{}'", info.key());
        return redisClient.tryInsertInDB(info.key(), info.lifetime());
    }


    @Override
    public void close() {
        log.info("Closing Redis client...");
        redisClient.close();

    }
}