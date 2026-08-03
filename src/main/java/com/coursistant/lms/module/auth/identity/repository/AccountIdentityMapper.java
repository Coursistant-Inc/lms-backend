package com.coursistant.lms.module.auth.identity.repository;

import com.coursistant.lms.module.auth.identity.entity.AccountIdentity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AccountIdentityMapper {

    @Select("SELECT id, normalized_email AS normalizedEmail, principal_type AS principalType, " +
            "principal_id AS principalId, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM account_identity WHERE normalized_email = #{email}")
    AccountIdentity selectByEmail(@Param("email") String email);

    @Insert("INSERT INTO account_identity (normalized_email, principal_type, principal_id) " +
            "VALUES (#{normalizedEmail}, #{principalType}, #{principalId})")
    int insert(AccountIdentity identity);
}
