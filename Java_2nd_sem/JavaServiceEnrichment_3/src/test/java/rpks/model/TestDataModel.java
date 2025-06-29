package rpks.model;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestDataModel {
    private String name;
    private Integer age;
    private String sex;
    @JsonRawValue
    private String enrichmentField;
    @JsonRawValue
    private String enrichmentOtherField;
}
