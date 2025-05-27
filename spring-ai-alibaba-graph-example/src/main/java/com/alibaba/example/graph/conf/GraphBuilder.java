package com.alibaba.example.graph.conf;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.QuestionClassifierNode;
import com.alibaba.example.graph.node.AnswerNode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

@Component
public class GraphBuilder {

    @Bean
    public CompiledGraph buildGraph(ChatModel chatModel) throws GraphStateException {
        // 1. 构造 ChatClient
        ChatClient chatClient = ChatClient.builder(chatModel)
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .build();

        // 2. 全局状态 & 注册键（key）及策略
        OverAllState state = new OverAllState();
        // 用户最初输入
        state.registerKeyAndStrategy("input",      (o1, o2) -> o2);
        // 分类结果
        state.registerKeyAndStrategy("classifier_output", (o1, o2) -> o2);
        // LLM 输出
        state.registerKeyAndStrategy("llm_response", (o1, o2) -> o2);
        // 最终回答
        state.registerKeyAndStrategy("answer",     (o1, o2) -> o2);

        // 3. 创建图
        StateGraph graph = new StateGraph(state);

        // —— 节点 1：问题分类 —— 
        QuestionClassifierNode qcNode = QuestionClassifierNode.builder()
            .chatClient(chatClient)
            // 从全局 state.input 中读用户问题
            .inputTextKey("input")
            // Categories & instructions 这里简化示例，实际可根据 DSL 填充
            .categories(List.of("售后服务", "运输", "产品质量", "其他"))
            .classificationInstructions(List.of("请根据类别对用户问题分类"))
            .build();
        // 分类输出写入 state.class_name
        graph.addNode("questionClassifier", AsyncNodeAction.node_async(qcNode));

        // —— 节点 2：调用 LLM —— 
        LlmNode llmNode = LlmNode.builder()
            // 如果你想传固定的 system 文本：
            .systemPromptTemplate("You are a helpful assistant.")
            // 从 state.class_name 里读取分类结果，做为后续 user prompt
            .userPromptTemplateKey("classifier_output")
            // LlmNode 内部根据 userPromptTemplateKey 自动去 state 里取值
            .outputKey("llm_response") // LLM 原始输出存入 state.llm_response
            .chatClient(chatClient)
            .build();
        graph.addNode("invokeLLM", AsyncNodeAction.node_async(llmNode));

        // —— 节点 3：Answer 输出 —— 
        AnswerNode ansNode = AnswerNode.builder()
            // 这里用 LLM 输出的 state.llm_response 生成最终 answer
            .answer("{{llm_response}}")
            .build();
        graph.addNode("answer", AsyncNodeAction.node_async(ansNode));

        // 4. 添加边
        graph
            .addEdge(START,               "questionClassifier")
            .addEdge("questionClassifier","invokeLLM")
            .addEdge("invokeLLM",         "answer")
            .addEdge("answer",            END);

        // 5. 编译
        return graph.compile();
    }
}
