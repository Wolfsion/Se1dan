package com.lavson.se1dan.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 审核请求
 *
 *  @author <a href="https://github.com/Wolfsion/">苏云</a>
 *  @from <a href="https://www.zhihu.com/people/dla-44-95">苏云不知云</a>
 */
@Data
public class ReviewRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 状态：0-待审核, 1-通过, 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;


    private static final long serialVersionUID = 1L;
}