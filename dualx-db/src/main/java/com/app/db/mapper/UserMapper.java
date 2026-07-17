package com.app.db.mapper;

import com.app.db.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

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


}
