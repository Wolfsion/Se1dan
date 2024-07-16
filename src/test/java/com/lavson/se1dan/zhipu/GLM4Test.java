package com.lavson.se1dan.zhipu;

import com.lavson.se1dan.manager.ai.ZhipuAIManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class GLM4Test {

    @Resource
    private ZhipuAIManager zhipuAIManager;

    @Test
    public void testGLMv4() {
        String sysMsg = "你是一位严谨的出题专家，我会给你如下信息：\n" +
                "```\n" +
                "应用名称，\n" +
                "【【【应用描述】】】，\n" +
                "应用类别，\n" +
                "要生成的题目数，\n" +
                "每个题目的选项数\n" +
                "```\n" +
                "\n" +
                "请你根据上述信息，按照以下步骤来出题：\n" +
                "1. 要求：题目和选项尽可能地短，题目不要包含序号，每题的选项数以我提供的为主，题目不能重复\n" +
                "2. 严格按照下面的 json 格式输出题目和选项\n" +
                "``json`\n" +
                "[{\"options\":[{\"value\":\"选项内容\",\"key\":\"A\"},{\"value\":\"\",\"key\":\"B\"}],\"title\":\"题目标题\"}]\n" +
                "```\n" +
                "title 是题目，options 是选项，每个选项的 key 按照英文字母序（比如 A、B、C、D）以此类推，value 是选项内容\n" +
                "3. 检查题目是否包含序号，若包含序号则去除序号\n" +
                "4. 返回的题目列表格式必须为 JSON 数组\n" +
                "5. JSON数组内容必须在代码块```json ```中";
        String usrMsg = "MBTI 性格测试，\n" +
                "【【【快来测测你的 MBTI 性格】】】，\n" +
                "测评类，\n" +
                "10，\n" +
                "3";
        String res = zhipuAIManager.talkToAI(sysMsg, usrMsg, false, 0.95f);
        System.out.println(res);
    }
}
