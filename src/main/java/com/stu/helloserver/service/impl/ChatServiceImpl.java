package com.stu.helloserver.service.impl;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.stu.helloserver.dto.ChatRequestDTO;
import com.stu.helloserver.service.ChatService;
import com.stu.helloserver.vo.ChatResponseVO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final StringRedisTemplate redisTemplate;

    // 保留最近几轮对话（每轮包含用户问题和助手回答）
    private static final int MAX_HISTORY_ROUNDS = 3;

    public ChatServiceImpl(ChatClient.Builder chatClientBuilder,
                           StringRedisTemplate redisTemplate) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一名专业、友好、简洁的中文智能助手，请结合历史对话上下文，连贯地回答用户的问题。")
                .defaultOptions(DashScopeChatOptions.builder()
                        .withTopP(0.7)
                        .build())
                .build();
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ChatResponseVO chat(ChatRequestDTO requestDTO) {
        String sessionId = requestDTO.getSessionId();
        String message = requestDTO.getMessage();

        // 若 sessionId 为空，使用默认值（避免错误）
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = "default";
        }

        String redisKey = "chat:session:" + sessionId;

        // 1. 读取历史消息（最近 MAX_HISTORY_ROUNDS 条记录，每条记录是 "用户：xxx\n助手：xxx" 格式）
        List<String> records = redisTemplate.opsForList().range(redisKey, 0, -1);
        StringBuilder historyBuilder = new StringBuilder();
        if (records != null && !records.isEmpty()) {
            for (String record : records) {
                historyBuilder.append(record).append("\n");
            }
        }
        String historyText = historyBuilder.toString();

        // 2. 拼接上下文（将历史对话和当前问题一起发给模型）
        String finalPrompt;
        if (historyText.isEmpty()) {
            finalPrompt = message;
        } else {
            finalPrompt = "以下是历史对话：\n" + historyText + "当前用户问题：" + message;
        }

        // 3. 调用大模型
        String answer = chatClient.prompt(finalPrompt).call().content();

        // 4. 保存本轮对话记录
        String record = "用户：" + message + "\n助手：" + answer;
        redisTemplate.opsForList().rightPush(redisKey, record);

        // 5. 只保留最近 MAX_HISTORY_ROUNDS 条记录
        Long size = redisTemplate.opsForList().size(redisKey);
        if (size != null && size > MAX_HISTORY_ROUNDS) {
            // 保留最后 MAX_HISTORY_ROUNDS 条，删除前面的
            redisTemplate.opsForList().trim(redisKey, size - MAX_HISTORY_ROUNDS, size - 1);
        }

        return new ChatResponseVO(message, answer);
    }
}