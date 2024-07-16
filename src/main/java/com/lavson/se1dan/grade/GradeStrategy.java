package com.lavson.se1dan.grade;

import com.lavson.se1dan.model.entity.App;
import com.lavson.se1dan.model.entity.UserAnswer;

import java.util.List;

public interface GradeStrategy {
    /**
     * 执行评分
     *
     * @param choices
     * @param app
     * @return
     * @throws Exception
     */
    UserAnswer doScore(List<String> choices, App app)throws Exception;
}
