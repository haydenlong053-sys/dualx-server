package com.app.db.mapper;

import com.app.db.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 用户 Mapper 接口
 * </p>
 *
 * @author HayDen
 * @since 2024-06-24
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    Integer findUserIdsByIds(@Param("keyList") List<String> keyList);

    User fineUserBuAmount(@Param("dayTime")LocalDateTime dayTime);

    /**
     * 查询用户下级总业绩（去掉最大区）
     * @param userId
     * @return
     */
    BigDecimal findAmountExcludeMax(@Param("userId")Integer userId);


    /**
     * 查询用户下级总业绩 AI（去掉最大区）
     * @param userId
     * @return
     */
    BigDecimal findAmountAiExcludeMax(@Param("userId")Integer userId);

    List<Integer> getIdList(@Param("size") Integer size, @Param("startId") Integer startId, @Param("minTeamAmount") BigDecimal minTeamAmount);
}
