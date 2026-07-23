package com.coursistant.lms.v2.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@Table(name = "nw_assignment")
public class AssignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant updatedAt;

    @Column(nullable = false, columnDefinition = "VARCHAR(63)", length = 63)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT", length = 1000)
    private String description;

    @Column(nullable = false, columnDefinition = "VARCHAR(31)", length = 31)
    private String type;

    @Column(name = "due_time", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant dueTime;

    @Convert(converter = SettingsConverter.class)
    @Column(nullable = false, columnDefinition = "JSON")
    private AssignmentSettings settings;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_unit_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_nw_assignment_course_unit"))
    @ToString.Exclude
    private CourseUnitEntity courseUnit;

    @Column(name="grade_published", nullable = false, columnDefinition = "TINYINT")
    private Integer gradePublish = 0;

    @Column(name="submission_required", nullable = false, columnDefinition = "TINYINT")
    private Integer submissionRequired = 1;

    @Column(name="is_group", nullable = false, columnDefinition = "TINYINT")
    private Integer isGroup = 0;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentSettings {
        private Boolean allowLateSubmission;
        private Integer allowedResubmissionCount;
    }

    @Converter
    public static class SettingsConverter implements AttributeConverter<AssignmentSettings, String> {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        static {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        @Override
        public String convertToDatabaseColumn(AssignmentSettings attribute) {
            try {
                return attribute == null ? null : OBJECT_MAPPER.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("JSON serialization failed", e);
            }
        }

        @Override
        public AssignmentSettings convertToEntityAttribute(String dbData) {
            try {
                return dbData == null ? null : OBJECT_MAPPER.readValue(dbData, AssignmentSettings.class);
            } catch (JsonProcessingException e) {
                return null;
            }
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AssignmentEntity that = (AssignmentEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
