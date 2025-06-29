package ru.mai.lessons.rpks.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mai.lessons.rpks.clients.EnrichmentClient;
import ru.mai.lessons.rpks.dto.request.EnrichmentRequest;
import ru.mai.lessons.rpks.dto.response.EnrichmentResponse;
import ru.mai.lessons.rpks.services.EnrichmentService;

@Service
@RequiredArgsConstructor
public class EnrichmentServiceImpl implements EnrichmentService {

  private final EnrichmentClient client;

  @Override
  public Iterable<EnrichmentResponse> getAllEnrichmentRequests() {
    return client.getAllEnrichments();
  }

  @Override
  public Iterable<EnrichmentResponse> getAllEnrichmentRequestsByEnrichmentRequestId(long id) {
    return client.getAllEnrichmentsByEnrichmentId(id);
  }

  @Override
  public EnrichmentResponse getEnrichmentRequestById(long enrichmentId, long ruleId) {
    return client.getEnrichmentById(enrichmentId, ruleId);
  }

  @Override
  public void deleteEnrichmentRequest() {
    client.deleteEnrichment();
  }

  @Override
  public void deleteEnrichmentRequestById(long enrichmentId, long ruleId) {
    client.deleteEnrichmentById(enrichmentId, ruleId);
  }

  @Override
  public void save(EnrichmentRequest enrichment) {
    client.save(enrichment);
  }
}
