package ru.mai.lessons.rpks.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mai.lessons.rpks.model.Deduplication;

import java.util.List;

@Repository
public interface DeduplicationRepository extends JpaRepository<Deduplication, Long> {

    public List<Deduplication> findAllByDeduplicationId(long id);

    public Deduplication findDeduplicationByDeduplicationIdAndRuleId(long deduplicationId, long ruleId);

    public void deleteDeduplicationByDeduplicationIdAndRuleId(long deduplicationId, long ruleId);
}