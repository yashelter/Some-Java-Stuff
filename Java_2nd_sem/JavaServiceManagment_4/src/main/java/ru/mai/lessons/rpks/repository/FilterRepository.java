package ru.mai.lessons.rpks.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mai.lessons.rpks.model.Filter;

import java.util.List;

@Repository
public interface FilterRepository extends JpaRepository<Filter, Long> {

    public List<Filter> getFilterByFilterId(long filterId);

    public Filter getFilterByFilterIdAndRuleId(long filterId, long ruleId);

    public void deleteAllByFilterIdAndRuleId(long id, long ruleId);
}