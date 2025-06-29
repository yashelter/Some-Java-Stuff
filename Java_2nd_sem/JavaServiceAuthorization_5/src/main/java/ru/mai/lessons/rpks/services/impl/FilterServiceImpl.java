package ru.mai.lessons.rpks.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mai.lessons.rpks.clients.FilterClient;
import ru.mai.lessons.rpks.dto.request.FilterRequest;
import ru.mai.lessons.rpks.dto.response.FilterResponse;
import ru.mai.lessons.rpks.services.FilterService;

@Service
@RequiredArgsConstructor
public class FilterServiceImpl implements FilterService {

  private final FilterClient client;

  @Override
  public Iterable<FilterResponse> getAllFilters() {
    return client.getAllFilters();
  }

  @Override
  public Iterable<FilterResponse> getAllFiltersByFilterId(long id) {
    return client.getAllFiltersByFilterId(id);
  }

  @Override
  public FilterResponse getFilterByFilterIdAndRuleId(long filterId, long ruleId) {
    return client.getFilterByFilterIdAndRuleId(filterId, ruleId);
  }

  @Override
  public void deleteFilter() {
    client.deleteFilter();
  }

  @Override
  public void deleteFilterById(long filterId, long ruleId) {
    client.deleteFilterById(filterId, ruleId);
  }

  @Override
  public void save(FilterRequest filter) {
    client.save(filter);
  }

}
