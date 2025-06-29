package ru.mai.lessons.rpks.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mai.lessons.rpks.model.Deduplication;
import ru.mai.lessons.rpks.repository.DeduplicationRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class DeduplicationService {

    private final DeduplicationRepository deduplicationRepository;

    public List<Deduplication> getAllDeduplications() {
        return deduplicationRepository.findAll();
    }

    public List<Deduplication> getAllDeduplicationsByDeduplicationId(long id) {
        return deduplicationRepository.findAllByDeduplicationId(id);
    }

    public Deduplication getDeduplicationById(long deduplicationId, long ruleId) {
        return deduplicationRepository.findDeduplicationByDeduplicationIdAndRuleId(deduplicationId, ruleId);
    }

    public void deleteDeduplication() {
        deduplicationRepository.deleteAll();
    }

    @Transactional
    public void deleteDeduplicationById(long deduplicationId, long ruleId) {
        deduplicationRepository.deleteDeduplicationByDeduplicationIdAndRuleId(deduplicationId, ruleId);
    }

    public void save(Deduplication deduplication) {
        deduplicationRepository.save(deduplication);
    }
}
