package com.coursistant.lms.v2.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@Table(name = "User")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, columnDefinition = "INT")
    private Integer id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "email")
    private String email;

    @Column(name = "password", length = 250)
    private String password;

    @Column(name = "must_change_password", columnDefinition = "TINYINT(1)")
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

    @Column(name = "status_id", columnDefinition = "INT")
    private Integer statusId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createAt;

    public enum Level {
        STUDENT, TEACHER
    }

    public enum Role {
        USER
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        UserEntity that = (UserEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}