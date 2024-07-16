package com.lavson.se1dan.grade;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lavson.se1dan.annotation.GradeStrategyMark;
import com.lavson.se1dan.model.dto.question.QuestionItemDTO;
import com.lavson.se1dan.model.entity.App;
import com.lavson.se1dan.model.entity.Question;
import com.lavson.se1dan.model.entity.ScoringResult;
import com.lavson.se1dan.model.entity.UserAnswer;
import com.lavson.se1dan.service.QuestionService;
import com.lavson.se1dan.service.ScoringResultService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.sql.Wrapper;
import java.util.List;
import java.util.Optional;

/**
 * quantization: 静态量化分析；人为给定评分策略，然后是得分计算
 *
 */
@Slf4j
@GradeStrategyMark(appType = 0, gradeType = 0)
public class StaticQuanStrategy implements GradeStrategy{
    @Resource
    private QuestionService questionService;

    @Resource
    private ScoringResultService scoringResultService;

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        // 1. 从app中导出应用对应题目的结果判题依据
        Long appId = app.getId();
        Question reference = questionService.getOne(
                Wrappers.lambdaQuery(Question.class)
                        .eq(Question::getAppId, appId)
        );
        List<QuestionItemDTO> qs = JSONUtil.toList(reference.getQuestionContent(), QuestionItemDTO.class);

        // 2. 遍历用户选项，然后根据依据进行累计加分
        int totalScore = 0;
        int len = choices.size();
        if (len != qs.size()) {
            log.error("用户答案不完全，缺省打分可能有误.");
        }

        for (int i = 0; i < len; i++) {
            String choice = choices.get(i);
            QuestionItemDTO q = qs.get(i);

            for (QuestionItemDTO.Option option : q.getOptions()) {
                if (choice.equals(option.getKey())) {
                    totalScore += Optional.of(option.getScore()).orElse(0);
                    break;
                }
            }
        }

        // 3. 根据用户得分，得到评价结论
        List<ScoringResult> assess = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                        .eq(ScoringResult::getAppId, appId)
                        .orderByDesc(ScoringResult::getResultScoreRange)
        );
        
        ScoringResult finalScore = assess.get(0);
        for (ScoringResult scoringResult : assess) {
            if (scoringResult.getResultScoreRange() <= totalScore) {
                finalScore = scoringResult;
                break;
            }
        }

        // 4. 收集结果返回
        UserAnswer ret = new UserAnswer();
        ret.setAppId(appId);
        ret.setAppType(app.getAppType());
        ret.setScoringStrategy(app.getScoringStrategy());
        ret.setChoices(JSONUtil.toJsonStr(choices));
        ret.setResultId(finalScore.getId());
        ret.setResultName(finalScore.getResultName());
        ret.setResultDesc(finalScore.getResultDesc());
        ret.setResultPicture(finalScore.getResultPicture());
        ret.setResultScore(totalScore);
        return ret;
    }
}
