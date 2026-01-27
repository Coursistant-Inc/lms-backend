package com.coursistant.lms.v2.repository;

import com.coursistant.lms.v2.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<UserEntity, Integer>,
        JpaSpecificationExecutor<UserEntity> {
}
