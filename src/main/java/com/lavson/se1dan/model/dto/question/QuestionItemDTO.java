package com.lavson.se1dan.model.dto.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionItemDTO {
    /**
     * 题目标题
     */
    private String title;

    /**
     * 题目选项列表
     */
    private List<Option> options;

    /**
     * 题目选项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Option {
        /**
         * 测评类使用的分类属性
         */
        private String result;
        /**
         * 得分类使用的计分属性
         */
        private int score;
        /**
         * 用户选项描述，给人看的
         */
        private String value;
        /**
         * 用户选项键，给机器用的
         */
        private String key;
    }
}