package com.lavson.se1dan.model.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建题目表请求
 *
 * @author <a href="https://github.com/Wolfsion/">苏云</a>
 * @from <a href="https://www.zhihu.com/people/dla-44-95">苏云不知云</a>
 */
@Data
public class QuestionAddRequest implements Serializable {

    /**
     * 题目内容（json格式）
     */
    private List<QuestionItemDTO> questionContent;

    /**
     * 应用 id
     */
    private Long appId;

    private static final long serialVersionUID = 1L;
}