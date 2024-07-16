package com.lavson.se1dan.grade;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lavson.se1dan.annotation.GradeStrategyMark;
import com.lavson.se1dan.common.ErrorCode;
import com.lavson.se1dan.constant.AIConstant;
import com.lavson.se1dan.constant.CommonConstant;
import com.lavson.se1dan.exception.ThrowUtils;
import com.lavson.se1dan.manager.ai.ZhipuAIManager;
import com.lavson.se1dan.model.dto.question.QuestionItemDTO;
import com.lavson.se1dan.model.dto.userAnswer.UserAnswerDTO;
import com.lavson.se1dan.model.entity.App;
import com.lavson.se1dan.model.entity.Question;
import com.lavson.se1dan.model.entity.UserAnswer;
import com.lavson.se1dan.model.vo.QuestionVO;
import com.lavson.se1dan.service.QuestionService;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * todo
 *
 * @author LA
 * @version 1.0
 * 2024/6/24 - 16:09
 */
@GradeStrategyMark(appType = 1, gradeType = 1)
public class AIQualStrategy implements GradeStrategy {
    @Resource
    private ZhipuAIManager zhipuAIManager;

    @Resource
    private QuestionService questionService;

    @Resource
    private RedissonClient redissonClient;

    private final Cache<String, String> answerCacheMap =
            Caffeine.newBuilder().initialCapacity(1024)
                    .expireAfterAccess(1L, TimeUnit.DAYS)
                    .build();

    private static final String AI_ANSWER_LOCK = AIConstant.AI_API_ANSWER_LOCK;

    private String getUsrMsg(App app, List<QuestionItemDTO> questionItemDTOList, List<String> choices) {
        StringBuilder sb = new StringBuilder();
        String appName = app.getAppName();
        String appDesc = app.getAppDesc();
        ThrowUtils.throwIf(questionItemDTOList.size() != choices.size(), ErrorCode.PARAMS_ERROR, "题目和用户回答的数量不一致");
        ArrayList<UserAnswerDTO> userAnswerDTOs = getUserAnswerDTOS(questionItemDTOList, choices);
        sb.append(appName).append("\n")
                .append("【【【").append(appDesc).append("】】】\n")
                .append(JSONUtil.toJsonStr(userAnswerDTOs));
        return sb.toString();
    }

    private String getCacheKey(Long appId, List<String> choices) {
        return appId + ":" + DigestUtil.md5Hex(JSONUtil.toJsonStr(choices));
    }

    private UserAnswer buildResponse(App app, List<String> choices, String answerJson) {
        UserAnswer userAnswer = JSONUtil.toBean(answerJson, UserAnswer.class);
        userAnswer.setAppId(app.getId());
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultPicture(CommonConstant.IMG_PLACEHOLDER);
        return userAnswer;
    }

    @NotNull
    private static ArrayList<UserAnswerDTO> getUserAnswerDTOS(List<QuestionItemDTO> questionItemDTOList, List<String> choices) {
        ArrayList<UserAnswerDTO> userAnswerDTOs = new ArrayList<>();
        for (int i = 0; i < choices.size(); i++) {
            QuestionItemDTO questionItemDTO = questionItemDTOList.get(i);
            String s = choices.get(i);
            UserAnswerDTO userAnswerDTO = new UserAnswerDTO();
            userAnswerDTO.setTitle(questionItemDTO.getTitle());
            for (QuestionItemDTO.Option option : questionItemDTO.getOptions()) {
                if (option.getKey().equals(s)) {
                    userAnswerDTO.setUserAnswer(option.getValue());
                    break;
                }
            }
            userAnswerDTOs.add(userAnswerDTO);
        }
        return userAnswerDTOs;
    }

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        Long appId = app.getId();
        String cacheKey = getCacheKey(appId, choices);
        RLock lock = redissonClient.getLock(AI_ANSWER_LOCK + cacheKey);
        String answerJson = answerCacheMap.getIfPresent(cacheKey);
        if (StrUtil.isNotBlank(answerJson)) {
            return buildResponse(app, choices, answerJson);
        }
        Question q = questionService.getOne(Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId));
        QuestionVO questionVO = QuestionVO.objToVo(q);
        List<QuestionItemDTO> qs = questionVO.getQuestionContent();
        String usrMsg = getUsrMsg(app, qs, choices);

        String result;
        try {
            lock.lock(15, TimeUnit.SECONDS);
            answerJson = answerCacheMap.getIfPresent(cacheKey);
            if (StrUtil.isNotBlank(answerJson)) {
                return buildResponse(app, choices, answerJson);
            }
            result = zhipuAIManager.talkToAIWithSync(AIConstant.AI_ANALYSIS, usrMsg, null);
        } finally {
            if (lock != null && lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        answerCacheMap.put(cacheKey, result);
        return buildResponse(app, choices, result);
    }
}
