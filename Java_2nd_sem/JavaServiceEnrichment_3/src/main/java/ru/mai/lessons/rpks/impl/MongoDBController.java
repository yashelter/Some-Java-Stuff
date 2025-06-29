package ru.mai.lessons.rpks.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import ru.mai.lessons.rpks.MongoDBClientEnricher;
import ru.mai.lessons.rpks.model.Rule;

import java.util.Objects;
@Slf4j
public class MongoDBController implements MongoDBClientEnricher, AutoCloseable {
    private static final String MONGO_ID_FIELD = "_id";
    private static final String OID_FIELD = "$oid";
    private static final String MONGO_CONFIG_PREFIX = "mongo.";

    private final MongoClient mongoController;
    private final MongoCollection<Document> collection;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public MongoDBController(Config config) {
        Objects.requireNonNull(config, "Config must not be null");

        final String connectionString = getConfigValue(config, "connectionString");
        final String databaseName = getConfigValue(config, "database");
        final String collectionName = getConfigValue(config, "collection");

        this.mongoController = MongoClients.create(connectionString);
        MongoDatabase database = mongoController.getDatabase(databaseName);
        this.collection = database.getCollection(collectionName);
    }


    private String getConfigValue(Config config, String path) {
        return config.getString(MONGO_CONFIG_PREFIX + path);
    }


    @Override
    public void enrichMessage(JsonNode messageInJson, Rule[] rules) {
        try {
            Objects.requireNonNull(messageInJson, "Message JSON must not be null");
            Objects.requireNonNull(rules, "Rules array must not be null");
        } catch (NullPointerException e) {
            log.error("Rules and Message must not be null", e);
            return;
        }

        log.info("Starting message enrichment process");
        final ObjectNode messageNode = (ObjectNode) messageInJson;

        for (Rule rule : rules) {
            try {
                processRule(messageNode, rule);
            } catch (Exception e) {
                log.error("Error processing rule {}: {}", rule.getRuleId(), e.getMessage());
            }
        }
    }


    private void processRule(ObjectNode messageNode, Rule rule) {
        final String searchingName = rule.getFieldNameEnrichment();
        final String searchingValue = rule.getFieldValue();
        final String targetField = rule.getFieldName();
        final String defaultValue = rule.getFieldValueDefault();

        log.debug("Appending rule {} for field {}", rule.getRuleId(), targetField);

        final Document document = collection.find(Filters.eq(searchingName, searchingValue))
                .sort(Sorts.descending(MONGO_ID_FIELD))
                .first();

        if (document != null) {
            messageNode.set(targetField, convertDocumentToJson(document));
        } else {
            log.warn("No document found for rule {}. Using default value", rule.getRuleId());
            messageNode.put(targetField, defaultValue);
        }
    }


    private JsonNode convertDocumentToJson(Document document) {
        final ObjectNode documentNode = objectMapper.valueToTree(document);
        final ObjectId id = document.getObjectId(MONGO_ID_FIELD);
        if (id != null) {
            documentNode.remove(MONGO_ID_FIELD);
            documentNode.putPOJO(MONGO_ID_FIELD, createOidNode(id));
        }

        return documentNode;
    }


    private ObjectNode createOidNode(ObjectId id) {
        return objectMapper.createObjectNode()
                .put(OID_FIELD, id.toString());
    }

    @Override
    public void close() {
        try {
            log.info("Closing MongoDB connection");
            mongoController.close();
        } catch (Exception e) {
            log.error("Error closing MongoDB client: {}", e.getMessage());
        }
    }
}