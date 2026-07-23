package com.coursistant.lms.v2.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Objects;

import org.hibernate.proxy.HibernateProxy;

import com.coursistant.lms.v2.entity.CourseUnitEntity;
import com.coursistant.lms.v2.entity.FileReferenceEntity;

@Data
@NoArgsConstructor
@ToString
@Entity
@Table(name = "nw_course_unit_content")
public class CourseUnitContentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_unit_id",
        referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_course_unit_content_course_unit"))
    @ToString.Exclude
    private CourseUnitEntity courseUnit;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id",
        referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_nw_course_unit_file_reference"))
    @ToString.Exclude
    private FileReferenceEntity fileReference;

    @Override
    public final boolean equals(Object o) {
        if(this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass(): this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        CourseUnitContentEntity that = (CourseUnitContentEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }


}
