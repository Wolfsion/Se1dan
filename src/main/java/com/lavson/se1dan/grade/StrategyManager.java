package com.lavson.se1dan.grade;

import com.lavson.se1dan.annotation.GradeStrategyMark;
import com.lavson.se1dan.common.ErrorCode;
import com.lavson.se1dan.exception.BusinessException;
import com.lavson.se1dan.model.entity.App;
import com.lavson.se1dan.model.entity.UserAnswer;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class StrategyManager {
    // 策略列表
    @Resource
    private List<GradeStrategy> strategyList;


    /**
     * 评分
     *
     * @param choiceList
     * @param app
     * @return
     * @throws Exception
     */
    public UserAnswer doGrade(List<String> choiceList, App app) throws Exception {
        Integer appType = app.getAppType();
        Integer appScoringStrategy = app.getScoringStrategy();
        if (appType == null || appScoringStrategy == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用配置有误，未找到匹配的策略");
        }
        // 根据注解获取策略
        for (GradeStrategy strategy : strategyList) {
            if (strategy.getClass().isAnnotationPresent(GradeStrategyMark.class)) {
                GradeStrategyMark strategyMark = strategy.getClass().getAnnotation(GradeStrategyMark.class);
                if (strategyMark.appType() == appType && strategyMark.gradeType() == appScoringStrategy) {
                    return strategy.doScore(choiceList, app);
                }
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用配置有误，未找到匹配的策略");
    }
}
