package ru.mai.lessons.rpks.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mai.lessons.rpks.model.Filter;
import ru.mai.lessons.rpks.repository.FilterRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class FilterService {
    private final FilterRepository filterRepository;

    public List<Filter> getAllFilters() {
        return filterRepository.findAll();
    }

    public List<Filter> getAllFiltersByFilterId(long filterId) {
        return filterRepository.getFilterByFilterId(filterId);
    }

    public Filter getFilterByFilterIdAndRuleId(long filterId, long ruleId) {
        return filterRepository.getFilterByFilterIdAndRuleId(filterId, ruleId);
    }

    public void deleteFilter() {
        filterRepository.deleteAll();
    }

    @Transactional
    public void deleteFilterById(long filterId, long ruleId) {
        filterRepository.deleteAllByFilterIdAndRuleId(filterId, ruleId);
    }

    public Filter save(Filter filter) {
        return filterRepository.save(filter);
    }

}