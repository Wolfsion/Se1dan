package com.lavson.se1dan.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lavson.se1dan.common.ErrorCode;
import com.lavson.se1dan.constant.AIConstant;
import com.lavson.se1dan.constant.CommonConstant;
import com.lavson.se1dan.exception.ThrowUtils;
import com.lavson.se1dan.manager.ai.ZhipuAIManager;
import com.lavson.se1dan.mapper.QuestionMapper;
import com.lavson.se1dan.model.dto.question.QuestionAiGenerateRequest;
import com.lavson.se1dan.model.dto.question.QuestionQueryRequest;
import com.lavson.se1dan.model.entity.App;
import com.lavson.se1dan.model.entity.Question;
import com.lavson.se1dan.model.entity.User;
import com.lavson.se1dan.model.enums.AppTypeEnum;
import com.lavson.se1dan.model.vo.QuestionVO;
import com.lavson.se1dan.model.vo.UserVO;
import com.lavson.se1dan.service.AppService;
import com.lavson.se1dan.service.QuestionService;
import com.lavson.se1dan.service.UserService;
import com.lavson.se1dan.utils.SqlUtils;
import com.zhipu.oapi.service.v4.model.ModelData;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 题目表服务实现
 *
 * @author <a href="https://github.com/Wolfsion/">苏云</a>
 * @from <a href="https://www.zhihu.com/people/dla-44-95">苏云不知云</a>
 */
@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    @Resource
    private ZhipuAIManager zhipuAIManager;

    /**
     * 校验数据
     *
     * @param question
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR);
        String questionContent = question.getQuestionContent();
        Long appId = question.getAppId();

        // 创建数据时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(questionContent), ErrorCode.PARAMS_ERROR, "题目内容不能为空");
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId 非法");
        }
        // 修改数据时，有参数则校验
        // 补充校验规则
        if (appId != null) {
            App app = appService.getById(appId);
            ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");
        }
    }


    @Override
    public void validQuestionAIGen(QuestionAiGenerateRequest aiGenerateRequest) {
        ThrowUtils.throwIf(aiGenerateRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = aiGenerateRequest.getAppId();
        Integer questionCnt = aiGenerateRequest.getQuestionCnt();
        Integer optionCnt = aiGenerateRequest.getOptionCnt();
        ThrowUtils.throwIf(appId == null || questionCnt == null || optionCnt == null ||
                appId <= 0 || questionCnt <= 0 || optionCnt <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");
    }

    public String getUsrMsg(App app, int questionCnt, int optionCnt) {
        StringBuilder sb = new StringBuilder();
        String appName = app.getAppName();
        String appDesc = app.getAppDesc();
        String appType = Objects.requireNonNull(AppTypeEnum.getEnumByValue(app.getAppType())).getText();
        sb.append(appName).append("\n").
                append("【【【").append(appDesc).append("】】】\n").
                append(appType).append("\n").
                append(questionCnt).append("\n")
                .append(optionCnt);
        return sb.toString();
    }

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = questionQueryRequest.getId();
        String questionContent = questionQueryRequest.getQuestionContent();
        Long appId = questionQueryRequest.getAppId();
        Long userId = questionQueryRequest.getUserId();
        Long notId = questionQueryRequest.getNotId();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        // 补充需要的查询条件
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(questionContent), "questionContent", questionContent);
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(appId), "appId", appId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题目表封装
     *
     * @param question
     * @param request
     * @return
     */
    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        // 对象转封装类
        QuestionVO questionVO = QuestionVO.objToVo(question);

        // 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionVO.setUser(userVO);
        // endregion

        return questionVO;
    }

    /**
     * 分页获取题目表封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            return QuestionVO.objToVo(question);
        }).collect(Collectors.toList());

        // 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        questionVOList.forEach(questionVO -> {
            Long userId = questionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUser(userService.getUserVO(user));
        });
        // endregion

        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    @Override
    public String addQuestionFromAISync(Long appId, Integer questionCnt, Integer optionCnt) {
        App app = appService.getById(appId);
        String usrMsg = getUsrMsg(app, questionCnt, optionCnt);
        return zhipuAIManager.talkToAIWithSync(AIConstant.AI_GEN_Qs, usrMsg, null);
    }

    @Override
    public void addQuestionFromAIStream(Long appId, Integer questionCnt, Integer optionCnt, SseEmitter sseEmitter) {
        App app = appService.getById(appId);
        String usrMsg = getUsrMsg(app, questionCnt, optionCnt);
        Flowable<ModelData> dataFlowable = zhipuAIManager.talkToAIStream(AIConstant.AI_GEN_Qs, usrMsg, null);

        StringBuilder singleQ = new StringBuilder();
        AtomicInteger balance = new AtomicInteger(0);
        dataFlowable.observeOn(Schedulers.io())
                .map(block -> block.getChoices().get(0).getDelta().getContent())
                .map(msg -> msg.replaceAll("\\s", ""))
                .filter(StrUtil::isNotBlank)
                .flatMap(msg -> {
                    List<Character> characters = new ArrayList<>();
                    char[] src = msg.toCharArray();
                    for (char c : src) {
                        characters.add(c);
                    }
                    return Flowable.fromIterable(characters);
                })
                .doOnNext(chr -> {
                    if (chr == '{') {
                        balance.addAndGet(1);
                    }

                    if (balance.get() > 0) {
                        singleQ.append(chr);
                    }

                    if (chr == '}') {
                        balance.addAndGet(-1);
                        if (balance.get() == 0) {
                            // 压缩当行json，消除换行符
                            sseEmitter.send(JSONUtil.toJsonStr(singleQ.toString()));
                            singleQ.setLength(0);
                        }
                    }

                })
                .doOnComplete(sseEmitter::complete)
                .subscribe();
    }


}
