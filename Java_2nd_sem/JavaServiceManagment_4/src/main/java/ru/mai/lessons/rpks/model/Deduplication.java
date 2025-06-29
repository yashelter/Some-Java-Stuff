package ru.mai.lessons.rpks.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "deduplication_rules")
public class Deduplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull
    @Positive
    private long id;

    @NotNull
    @Positive
    private long deduplicationId;

    @NotNull
    @Positive
    private long ruleId;

    @NotNull
    @NotBlank
    private String fieldName;

    @NotNull
    @Positive
    private long timeToLiveSec;

    @NotNull
    private boolean isActive;
}
