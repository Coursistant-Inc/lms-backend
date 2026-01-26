package com.coursistant.lms.v2.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "`User`",
        indexes = {
                @Index(name = "email", columnList = "email", unique = true)
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "email", columnNames = {"email"})
        })
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, columnDefinition = "int")
    private Integer id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password", length = 250)
    private String password;

    @Column(name = "must_change_password", columnDefinition = "tinyint(1)")
    @ColumnDefault("0")
    @Comment("1 = must change password on next login, 0 = no requirement")
    private Boolean mustChangePassword;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    private Level level;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @ColumnDefault("'USER'")
    private Role role;

    @Column(name = "invitation")
    private String invitation;

    @Column(name = "status_id", columnDefinition = "int")
    private Integer statusId;

    @Column(name = "create_at", nullable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createAt;

    public enum Level {
        STUDENT, TEACHER
    }

    public enum Role {
        USER,
        ADMIN
    }

    @PrePersist
    protected void onCreate() {
        if (createAt == null) {
            createAt = LocalDateTime.now();
        }
        if (mustChangePassword == null) {
            mustChangePassword = false;
        }
        if (role == null) {
            role = Role.USER;
        }
    }
}