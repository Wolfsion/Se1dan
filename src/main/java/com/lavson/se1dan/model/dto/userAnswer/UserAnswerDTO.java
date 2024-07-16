package com.lavson.se1dan.model.dto.userAnswer;

import lombok.Data;

@Data
public class UserAnswerDTO {
    /**
     * 题目
     */
    private String title;

    /**
     * 用户答案
     */
    private String userAnswer;
}
