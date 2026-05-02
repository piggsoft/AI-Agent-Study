package com.piggsoft.webchat.mapper;

import com.piggsoft.webchat.entity.AiConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface AiConfigMapper {

    Optional<AiConfig> selectByConfigKey(@Param("configKey") String configKey);
}
