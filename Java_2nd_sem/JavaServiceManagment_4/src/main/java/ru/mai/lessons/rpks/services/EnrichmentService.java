package ru.mai.lessons.rpks.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mai.lessons.rpks.model.Enrichment;
import ru.mai.lessons.rpks.repository.EnrichmentRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class EnrichmentService {

    private final EnrichmentRepository enrichmentRepository;

    public List<Enrichment> getAllEnrichments() {
        return enrichmentRepository.findAll();
    }

    public List<Enrichment> getAllEnrichmentsByEnrichmentId(long id) {
        return enrichmentRepository.getEnrichmentByEnrichmentId(id);
    }

    public Enrichment getEnrichmentById(long id, long ruleId) {
        return enrichmentRepository.getEnrichmentByEnrichmentIdAndRuleId(id, ruleId);
    }

    public void deleteEnrichment() {
        enrichmentRepository.deleteAll();
    }

    @Transactional
    public void deleteEnrichmentById(long id, long ruleId) {
        enrichmentRepository.deleteEnrichmentByEnrichmentIdAndRuleId(id, ruleId);
    }

    public void save(Enrichment enrichment) {
        enrichmentRepository.save(enrichment);
    }
}