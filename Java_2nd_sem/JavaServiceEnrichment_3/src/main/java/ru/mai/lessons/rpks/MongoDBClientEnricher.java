package ru.mai.lessons.rpks;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mai.lessons.rpks.model.Rule;


public interface MongoDBClientEnricher {
    public void enrichMessage(JsonNode messageInJson, Rule[] rules);

    void close();
}
