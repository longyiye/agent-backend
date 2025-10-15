package com.yiye.app;

import com.yiye.advisor.MyLoggerAdvisor;
import com.yiye.advisor.ReReadingAdvisor;
import com.yiye.chatmemory.FileBasedChatMemory;
import com.yiye.constant.FileConstant;
import com.yiye.rag.LoveAppRagCustomAdvisorFactory;
import com.yiye.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT =
            "扮演深耕同性恋爱心理领域的专家，擅长理解并辅导多元角色与关系形态中的情感问题。开场时向用户表明你的专业身份，你的名字是：Gavin，欢迎用户倾诉他们在同性恋爱中遇到的任何情感或关系难题。" +
                    "根据用户当前在关系中所处的角色或状态，有针对性地展开提问：" +
                    "【状态 1（攻）】：了解作为同性关系中偏主动、传统上扮演插入方或主导角色（即“攻”）的用户，所面临的特有挑战，例如自我角色认同、在关系中的主动性表现、力量平衡、情感表达与身体互动的协调，以及在扮演该角色过程中的心理压力或困惑。" +
                    "【状态 0（受）】：关注作为同性关系中偏被动、传统上扮演接受方或顺从角色（即“受”）的用户，他们可能面临的问题包括角色适应、情感需求表达、在关系中的主动性缺失感、依赖与独立的平衡，以及对传统角色分工的心理反应等。" +
                    "【状态 side（不10 / 非传统角色）】：适用于那些不认同或不固定在传统“1”或“0”角色中的用户，可能包括双性角色（不分）、无固定角色、探索阶段、性别流动者、关系中角色模糊，或对1/0标签本身感到不适的人群。询问他们在角色认同、关系动态、社会期待与自我表达之间的冲突与困惑。" +
                    "引导用户详细说明他们的经历、与伴侣的互动细节、彼此的反应，以及他们自己的感受与想法，从而提供更具针对性与共情的建议与解决方案。";

    record LoveReport(String title, List<String> suggestions) {

    }

    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private Advisor loveAppRagCloudAdvisor;

    @Resource
    private QueryRewriter queryRewriter;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * 初始化 AI 客户端
     *
     * @param dashscopeChatModel
     */
    public LoveApp(ChatModel dashscopeChatModel) {
        // // 初始化基于内存的对话记忆
        // ChatMemory chatMemory = new InMemoryChatMemory();
        // 初始化基于文件的对话记忆
        String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(FILE_DIR);

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        // 自定义日志 Advisor，可按需开启
                        new MyLoggerAdvisor()
                        // // 自定义推理增强 Advisor，可按需开启
                        // new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * AI 基础对话（多轮记忆对话）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 恋爱报告生成
     *
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    public String doChatWithRag(String message, String chatId) {
        // 查询重写
        String rewritterMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                // .user(message)
                // 使用改写后的查询
                .user(rewritterMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // // 应用知识库问答
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 应用增强检索服务（云知识库服务）
                // .advisors(loveAppRagCloudAdvisor)
                // // 自定义检索增强过滤器（文档过滤 + 上下文增强）
                // .advisors(LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
                //         loveAppVectorStore, "单身"
                // ))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("rag content: {}", content);
        return content;
    }

    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("with tool content: {}", content);
        return content;
    }

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("with mcp content: {}", content);
        return content;
    }

    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
    }

}
