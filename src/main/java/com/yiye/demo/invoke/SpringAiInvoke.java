package com.yiye.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * SpringAi 测试
 * 取消注释 @Component 即可在 SpringBoot 项目启动时执行
 * CommandLineRunner 可以在项目启动后运行一次 run 方法
 */
// @Component
public class SpringAiInvoke implements CommandLineRunner {

    @Resource
    private ChatModel dashscopeChatModel;

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage output = dashscopeChatModel.call(new Prompt("你好，我是小野狼"))
                .getResult()
                .getOutput();
        System.out.println(output.getText());
    }

}
