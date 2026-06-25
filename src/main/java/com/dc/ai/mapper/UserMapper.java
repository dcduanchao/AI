package com.dc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dc.ai.domain.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("""
            SELECT id,
                   username,
                   password,
                   nickname,
                   enabled,
                   created_at,
                   updated_at
            FROM sys_user
            WHERE username = #{username}
              AND enabled = 1
            LIMIT 1
            """)
    UserEntity selectByUsername(@Param("username") String username);
}
