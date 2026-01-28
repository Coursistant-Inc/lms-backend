package com.coursistant.lms.v2.common;

import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.SimpleExpression;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.function.Function;

@Data
@Getter
@AllArgsConstructor
public class EntityRepositoryMapping<T> {
    private JpaRepository<T, Long> repository;
    private EntityPathBase<T> qEntity;
    private Function<EntityPathBase<T>, SimpleExpression<Long>> idPathGetter;

    public SimpleExpression<Long> getIdPath() {
        return idPathGetter.apply(qEntity);
    }
}