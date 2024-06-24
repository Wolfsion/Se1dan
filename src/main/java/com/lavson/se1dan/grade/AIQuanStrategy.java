package com.lavson.se1dan.grade;

import com.lavson.se1dan.annotation.GradeStrategyMark;
import com.lavson.se1dan.model.entity.App;
import com.lavson.se1dan.model.entity.UserAnswer;

import java.util.List;

/**
 * todo
 *
 * @author LA
 * @version 1.0
 * 2024/6/24 - 16:10
 */
@GradeStrategyMark(appType = 0, gradeType = 1)
public class AIQuanStrategy implements GradeStrategy{
    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        return null;
    }
}
