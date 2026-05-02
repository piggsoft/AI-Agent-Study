package com.piggsoft.webchat.mapper;

import com.piggsoft.webchat.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatHistoryMapper {

    List<ChatHistory> selectBySessionId(@Param("sessionId") String sessionId);

    int insert(ChatHistory chatHistory);
}
