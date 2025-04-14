package com.coursistant.individual.mapper.system;

import com.coursistant.individual.entity.RefreshToken;


import java.util.List;

/**
 * 操作 refreshToken 相关数据接口
 * Data access interface for refreshToken-related operations
 */
public interface RefreshTokenMapper {

    int insert(RefreshToken refreshToken);
    int deleteById(Integer id);
    int deleteByUserId(Integer userId);
    int deleteExpiredTokens();

    int updateById(RefreshToken refreshToken);

    RefreshToken selectById(Integer id);
    RefreshToken selectByUserId(Integer userId);
    RefreshToken selectByToken(String token);


    List<RefreshToken> selectAllByUserId(Integer userId);


    List<RefreshToken> selectByUserIdAndRoleOrderByCreateTime(Integer userId, String role);
}
