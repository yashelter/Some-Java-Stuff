package ru.mai.lessons.rpks.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class EnrichmentRequest {

  private long id;
  private long enrichmentId;
  private long ruleId;
  private String fieldName;
  private String fieldNameEnrichment;
  private String fieldValue;
  private String fieldValueDefault;
}
