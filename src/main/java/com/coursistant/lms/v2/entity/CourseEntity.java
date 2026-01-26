package com.coursistant.lms.v2.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@Table(name = "nw_course")
public class CourseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant updatedAt;

    @Column(name = "course_code", nullable = false, columnDefinition = "VARCHAR(16)", length = 16)
    private String courseCode;

    @Column(nullable = false, columnDefinition = "VARCHAR(127)", length = 127)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT", length = 1000)
    private String description;

    @Column(nullable = false, columnDefinition = "VARCHAR(127)", length = 127)
    private String school;

    @Column(nullable = false, columnDefinition = "VARCHAR(127)", length = 127)
    private String semester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id",
            referencedColumnName = "id",
            columnDefinition = "INT",
            foreignKey = @ForeignKey(name = "fk_nw_course_teacher"))
    private UserEntity teacher;

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
