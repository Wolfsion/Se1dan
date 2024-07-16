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
import java.util.HashMap;
import java.util.List;

/**
 * qualitative: 静态定性分析；人为给定分类依据，然后是测评分类分析
 *
 */
@Slf4j
@GradeStrategyMark(appType = 1, gradeType = 0)
public class StaticQualStrategy implements GradeStrategy{
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
        
        // 2. 根据用户答案进行分类属性计数
        HashMap<String, Integer> classProperties = new HashMap<>();
        int len = choices.size();
        if (len != qs.size()) {
            log.error("用户答案不完全，缺省打分可能有误.");
        }

        for (int i = 0; i < len; i++) {
            String choice = choices.get(i);
            QuestionItemDTO q = qs.get(i);

            for (QuestionItemDTO.Option option : q.getOptions()) {
                if (choice.equals(option.getKey())) {
                    String result = option.getResult();
                    classProperties.put(result, classProperties.getOrDefault(result, 0)+1);
                }
            }
        }
        
        // 3. 从判题依据中取到应用所有分类结果，通过各个属性累加得出最终分类
        int maxScore = 0;
        List<ScoringResult> assess = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                        .eq(ScoringResult::getAppId, appId)
        );
        ScoringResult scoringResult = assess.get(0);
        for (ScoringResult result : assess) {
            String resultProp = result.getResultProp();
            List<String> properties = JSONUtil.toList(resultProp, String.class);
            int curt = properties.stream().
                    mapToInt(prop -> classProperties.getOrDefault(prop, 0)).
                    sum();
            if (curt > maxScore) {
                maxScore = curt;
                scoringResult = result;
            }
        }

        // 4. 收集整理结果
        UserAnswer ret = new UserAnswer();
        ret.setAppId(appId);
        ret.setAppType(app.getAppType());
        ret.setScoringStrategy(app.getScoringStrategy());
        ret.setChoices(JSONUtil.toJsonStr(choices));
        ret.setResultId(scoringResult.getId());
        ret.setResultName(scoringResult.getResultName());
        ret.setResultDesc(scoringResult.getResultDesc());
        ret.setResultPicture(scoringResult.getResultPicture());
        return ret;
    }
}
