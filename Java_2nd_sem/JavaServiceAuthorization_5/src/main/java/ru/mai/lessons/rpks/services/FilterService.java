package ru.mai.lessons.rpks.services;

import ru.mai.lessons.rpks.dto.request.FilterRequest;
import ru.mai.lessons.rpks.dto.response.FilterResponse;

public interface FilterService {

  Iterable<FilterResponse> getAllFilters();

  Iterable<FilterResponse> getAllFiltersByFilterId(long id);

  FilterResponse getFilterByFilterIdAndRuleId(long filterId, long ruleId);

  void deleteFilter();

  void deleteFilterById(long filterId, long ruleId);

  void save(FilterRequest filter);
}
