package com.dc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dc.ai.domain.ProviderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProviderMapper extends BaseMapper<ProviderEntity> {

    @Select("""
            SELECT id,
                   ai_code AS code,
                   ai_name AS name,
                   adapter_type,
                   base_url,
                   api_key,
                   enabled,
                   config_json,
                   created_at,
                   updated_at
            FROM ai_provider
            WHERE enabled = 1
            ORDER BY ai_name ASC
            """)
    List<ProviderEntity> selectEnabledOrderByNameAsc();

    @Select("""
            SELECT id,
                   ai_code AS code,
                   ai_name AS name,
                   adapter_type,
                   base_url,
                   api_key,
                   enabled,
                   config_json,
                   created_at,
                   updated_at
            FROM ai_provider
            WHERE ai_code = #{aiCode}
              AND enabled = 1
            LIMIT 1
            """)
    ProviderEntity selectEnabledByAiCode(@Param("aiCode") String aiCode);
}
