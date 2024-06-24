package com.lavson.se1dan.grade;

import com.lavson.se1dan.annotation.GradeStrategyMark;
import com.lavson.se1dan.model.entity.App;
import com.lavson.se1dan.model.entity.UserAnswer;

import java.util.List;

/**
 * qualitative: 静态定性分析；人为给定分类依据，然后是测评分类分析
 *
 */
@GradeStrategyMark(appType = 1, gradeType = 0)
public class StaticQualStrategy implements GradeStrategy{
    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        return null;
    }
}
