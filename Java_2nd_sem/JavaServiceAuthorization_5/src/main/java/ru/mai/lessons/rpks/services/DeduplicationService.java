package ru.mai.lessons.rpks.services;

import ru.mai.lessons.rpks.dto.request.DeduplicationRequest;
import ru.mai.lessons.rpks.dto.response.DeduplicationResponse;

public interface DeduplicationService {

  Iterable<DeduplicationResponse> getAllDeduplications();

  Iterable<DeduplicationResponse> getAllDeduplicationsByDeduplicationId(long id);

  DeduplicationResponse getDeduplicationById(long deduplicationId, long ruleId);

  void deleteDeduplication();

  void deleteDeduplicationById(long deduplicationId, long ruleId);

  void save(DeduplicationRequest deduplication);
}