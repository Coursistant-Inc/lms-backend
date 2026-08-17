package com.coursistant.lms.module.auth.token.repository;

import com.coursistant.lms.module.auth.token.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 操作 refreshToken 相关数据接口
 * Data access interface for refreshToken-related operations
 */
@Mapper
public interface RefreshTokenMapper {

    int insert(RefreshToken refreshToken);
    int deleteById(Integer id);
    int deleteByUserId(Integer userId);
    int deleteByToken(String token);
    int deleteBySessionId(String sessionId);
    int deleteExpiredTokens();

    int updateById(RefreshToken refreshToken);

    RefreshToken selectById(Integer id);
    RefreshToken selectByUserId(Integer userId);
    RefreshToken selectByToken(String token);
    RefreshToken selectBySessionIdForUpdate(String sessionId);

    List<RefreshToken> selectAllByUserId(Integer userId);

    List<RefreshToken> selectByUserIdAndRoleOrderByCreateTime(Integer userId, String role);
}
