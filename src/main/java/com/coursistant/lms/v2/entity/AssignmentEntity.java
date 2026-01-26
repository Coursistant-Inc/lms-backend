package com.coursistant.lms.v2.entity;

import jakarta.persistence.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@Table(name = "nw_assignment")
public class AssignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant updatedAt;

    @Column(nullable = false, columnDefinition = "VARCHAR(63)", length = 63)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT", length = 1000)
    private String description;

    @Column(nullable = false, columnDefinition = "VARCHAR(31)", length = 31)
    private String type;

    @Column(name = "due_time", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant dueTime;

    @Convert(converter = SettingsConverter.class)
    @Column(nullable = false, columnDefinition = "JSON")
    private AssignmentSettings settings;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_unit_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "fk_nw_assignment_course_unit",
                    foreignKeyDefinition = "FOREIGN KEY (course_unit_id) REFERENCES nw_course_unit(id) ON DELETE NO ACTION"
            ))
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private CourseUnitEntity courseUnit;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @Converter
    public static class SettingsConverter implements AttributeConverter<AssignmentSettings, String> {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        @Override
        @SneakyThrows
        public String convertToDatabaseColumn(AssignmentSettings attribute) {
            return attribute == null ? null : OBJECT_MAPPER.writeValueAsString(attribute);
        }

        @Override
        @SneakyThrows
        public AssignmentSettings convertToEntityAttribute(String dbData) {
            return dbData == null ? null : OBJECT_MAPPER.readValue(dbData, AssignmentSettings.class);
        }
    }

    @Data
    public static class AssignmentSettings {
        private Boolean allowLateSubmission;
        private Integer allowedResubmissionCount;
    }
}
