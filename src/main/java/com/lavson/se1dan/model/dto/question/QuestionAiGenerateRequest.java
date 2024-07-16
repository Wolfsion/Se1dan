package com.lavson.se1dan.model.dto.question;

import lombok.Data;
import scala.Int;

import java.io.Serializable;

@Data
public class QuestionAiGenerateRequest implements Serializable {
    /**
     * 应用id
     */
    private Long appId;

    /**
     * 生成题目数
     */
    private Integer questionCnt;

    /**
     * 题目选项数
     */
    private Integer optionCnt;

    private static final long serialVersionUID = 1L;
}
