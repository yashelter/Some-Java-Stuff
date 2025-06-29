package ru.mai.lessons.rpks.services;

import ru.mai.lessons.rpks.dto.request.EnrichmentRequest;
import ru.mai.lessons.rpks.dto.response.EnrichmentResponse;

public interface EnrichmentService {

  Iterable<EnrichmentResponse> getAllEnrichmentRequests();

  Iterable<EnrichmentResponse> getAllEnrichmentRequestsByEnrichmentRequestId(long id);

  EnrichmentResponse getEnrichmentRequestById(long enrichmentId, long ruleId);

  void deleteEnrichmentRequest();

  void deleteEnrichmentRequestById(long enrichmentId, long ruleId);

  void save(EnrichmentRequest enrichment);
}
