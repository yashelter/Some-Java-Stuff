package ru.mai.lessons.rpks.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mai.lessons.rpks.clients.DeduplicationClient;
import ru.mai.lessons.rpks.dto.request.DeduplicationRequest;
import ru.mai.lessons.rpks.dto.response.DeduplicationResponse;
import ru.mai.lessons.rpks.services.DeduplicationService;

@Service
@RequiredArgsConstructor
public class DeduplicationServiceImpl implements DeduplicationService {

  private final DeduplicationClient client;

  @Override
  public Iterable<DeduplicationResponse> getAllDeduplications() {
    return client.getAllDeduplications();
  }

  @Override
  public Iterable<DeduplicationResponse> getAllDeduplicationsByDeduplicationId(long id) {
    return client.getAllDeduplicationsByDeduplicationId(id);
  }

  @Override
  public DeduplicationResponse getDeduplicationById(long deduplicationId, long ruleId) {
    return client.getDeduplicationById(deduplicationId, ruleId);
  }

  @Override
  public void deleteDeduplication() {
    client.deleteDeduplication();
  }

  @Override
  public void deleteDeduplicationById(long deduplicationId, long ruleId) {
    client.deleteDeduplicationById(deduplicationId, ruleId);
  }

  @Override
  public void save(DeduplicationRequest deduplication) {
    client.save(deduplication);
  }
}
