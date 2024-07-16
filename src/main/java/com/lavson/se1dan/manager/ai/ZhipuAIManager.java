package com.lavson.se1dan.manager.ai;

import com.lavson.se1dan.common.ErrorCode;
import com.lavson.se1dan.exception.BusinessException;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ZhipuAIManager {

    @Resource
    private ClientV4 clientV4;

    // 稳定的随机数
    private static final Float STABLE_TEMPERATURE = 0.05f;

    // 不稳定的随机数
    private static final Float UNSTABLE_TEMPERATURE = 0.99f;

    private static final Pattern regexPattern1 = Pattern.compile("```json(.*?)```", Pattern.DOTALL);
    private static final Pattern regexPattern2 = Pattern.compile("(\\[.*\\])", Pattern.DOTALL);
    private static final Pattern regexPattern3 = Pattern.compile("(\\{.*\\})", Pattern.DOTALL);
    public static String extractJson(String input) {
        // Create a matcher for the input string
        Matcher matcher = regexPattern1.matcher(input);
        // Check if any match is found
        if (matcher.find()) {
            // Return the content between the delimiters
            return matcher.group(1).trim();
        } else {
            // Second pattern: Between the first [ and the last ]
            matcher = regexPattern2.matcher(input);
            if (matcher.find()) {
                return matcher.group(1).trim();
            } else {
                // Third pattern: Between the first { and the last }
                matcher = regexPattern3.matcher(input);
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            }
        }
        return null; // Return null if no match is found
    }

    public List<ChatMessage> buildMses(String sysMsg, String usrMsg) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage systemChatMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), sysMsg);
        ChatMessage userChatMessage = new ChatMessage(ChatMessageRole.USER.value(), usrMsg);
        messages.add(systemChatMessage);
        messages.add(userChatMessage);
        return messages;
    }

    public String talkToAI(List<ChatMessage> messages, boolean streamEnabled, Float temperature) {
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(streamEnabled)
                .temperature(temperature)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .build();
        try {
            ModelApiResponse invokeModelApiResp = clientV4.invokeModelApi(chatCompletionRequest);
            String oriRes = invokeModelApiResp.getData().getChoices().get(0).toString();
            String res = extractJson(oriRes);
            if (res == null) {
                throw new RuntimeException("No JSON found in response");
            } else {
                return res;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("syncTalk error: {}", "ZhipuAI API call failed");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }

    public String talkToAI(String sysMsg, String usrMsg, boolean streamEnabled, Float temperature) {
        List<ChatMessage> messages = buildMses(sysMsg, usrMsg);
        return talkToAI(messages, streamEnabled, temperature);
    }

    public String talkToAIWithSync(String sysMsg, String usrMsg, Float temperature) {
        return talkToAI(sysMsg, usrMsg, Boolean.FALSE, temperature);
    }

    public String talkToAIWithSyncStable(String sysMsg, String usrMsg) {
        return talkToAI(sysMsg, usrMsg, Boolean.FALSE, STABLE_TEMPERATURE);
    }

    public String talkToAIWithSyncUnStable(String sysMsg, String usrMsg) {
        return talkToAI(sysMsg, usrMsg, Boolean.FALSE, UNSTABLE_TEMPERATURE);
    }

    public Flowable<ModelData> talkToAIStream(List<ChatMessage> messages, Float temperature) {
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.TRUE)
                .temperature(temperature)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .build();
        ModelApiResponse invokeModelApiResp = clientV4.invokeModelApi(chatCompletionRequest);
        return invokeModelApiResp.getFlowable();
    }

    public Flowable<ModelData> talkToAIStream(String sysMsg, String usrMsg, Float temperature) {
        List<ChatMessage> messages = buildMses(sysMsg, usrMsg);
        return talkToAIStream(messages, temperature);
    }
}
