package com.coursistant.lms.v2.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@Table(name = "nw_course_unit")
public class CourseUnitEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant updatedAt;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false, columnDefinition = "VARCHAR(63)", length = 63)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT", length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "fk_nw_course_unit_course",
                    foreignKeyDefinition = "FOREIGN KEY (course_id) REFERENCES nw_course(id) ON DELETE NO ACTION"
            ))
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private CourseEntity course;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
