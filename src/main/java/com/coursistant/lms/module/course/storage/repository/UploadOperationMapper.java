package com.coursistant.lms.module.course.storage.repository;

import com.coursistant.lms.module.course.storage.entity.UploadOperation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UploadOperationMapper {
    int insert(UploadOperation row);

    UploadOperation selectByActorKeyRoute(@Param("actorType") String actorType,
                                          @Param("actorId") Integer actorId,
                                          @Param("idempotencyKey") String idempotencyKey,
                                          @Param("routeId") String routeId);

    UploadOperation selectById(@Param("id") String id);

    int updateStatus(@Param("id") String id,
                     @Param("status") String status,
                     @Param("visibilityStatus") String visibilityStatus);
}
