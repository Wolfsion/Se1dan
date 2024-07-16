package com.lavson.se1dan.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lavson.se1dan.model.dto.question.QuestionAiGenerateRequest;
import com.lavson.se1dan.model.dto.question.QuestionQueryRequest;
import com.lavson.se1dan.model.entity.Question;
import com.lavson.se1dan.model.vo.QuestionVO;
import io.reactivex.Flowable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;

/**
 * 题目表服务
 *
 * @author <a href="https://github.com/Wolfsion/">苏云</a>
 * @from <a href="https://www.zhihu.com/people/dla-44-95">苏云不知云</a>
 */
public interface QuestionService extends IService<Question> {

    /**
     * 校验数据
     *
     * @param question
     * @param add 对创建的数据进行校验
     */
    void validQuestion(Question question, boolean add);

    void validQuestionAIGen(QuestionAiGenerateRequest aiGenerateRequest);

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest);
    
    /**
     * 获取题目表封装
     *
     * @param question
     * @param request
     * @return
     */
    QuestionVO getQuestionVO(Question question, HttpServletRequest request);

    /**
     * 分页获取题目表封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request);

    String addQuestionFromAISync(Long appId, Integer questionCnt, Integer optionCnt);

    void addQuestionFromAIStream(Long appId, Integer questionCnt, Integer optionCnt, SseEmitter sseEmitter);
}
