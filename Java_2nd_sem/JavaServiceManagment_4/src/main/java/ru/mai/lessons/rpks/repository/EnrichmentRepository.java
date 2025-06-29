package ru.mai.lessons.rpks.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mai.lessons.rpks.model.Enrichment;

import java.util.List;

@Repository
public interface EnrichmentRepository extends JpaRepository<Enrichment, Long> {

    public List<Enrichment> getEnrichmentByEnrichmentId(long enrichmentId);

    public Enrichment getEnrichmentByEnrichmentIdAndRuleId(long enrichmentId, long ruleId);

    public void deleteEnrichmentByEnrichmentIdAndRuleId(long enrichmentId, long ruleId);
}