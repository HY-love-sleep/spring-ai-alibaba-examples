package com.alibaba.example.graph.conf;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.*;
import com.alibaba.example.graph.node.AnswerNode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.StateGraph.END;

@Component
public class ComplexSupportGraphBuilder {

    @Bean
    public CompiledGraph buildGraph(ChatModel chatModel,
                                    VectorStore vectorStore,
                                    ToolCallbackResolver toolCallbackResolver) throws GraphStateException {

        // ChatClient
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();

        OverAllStateFactory stateFactory = () -> {
            OverAllState state = new OverAllState();
            for (String key : List.of(
                    "input",
                    "attachments",
                    "docs",
                    "parameters",
                    "classifier_output",
                    "retrieved_docs",
                    "filtered_docs",
                    "http_response",
                    "llm_response",
                    "tool_result",
                    "human_feedback",
                    "answer"
            )) {
                state.registerKeyAndStrategy(key, (o1, o2) -> o2);
            }
            return state;
        };

        StateGraph graph = new StateGraph(stateFactory);

        // —— 1. Document extraction ——
        DocumentExtractorNode extractNode = DocumentExtractorNode.builder()
                .fileList(List.of("data/manual.md"))
                .paramsKey("attachments")
                .outputKey("docs")
                .build();
        graph.addNode("extractDocs", AsyncNodeAction.node_async(extractNode));

        // —— 2. Parameter parsing ——
        ParameterParsingNode paramNode = ParameterParsingNode.builder()
                .chatClient(chatClient)
                .inputTextKey("input")
                .parameters(List.of(
                        Map.of("name", "ticketId", "type", "string", "description", "工单编号"),
                        Map.of("name", "priority", "type", "string", "description", "优先级")
                ))
                .build();
        graph.addNode("parseParams", AsyncNodeAction.node_async(paramNode));

        // —— 3. Classification of issues ——
        QuestionClassifierNode qcNode = QuestionClassifierNode.builder()
                .chatClient(chatClient)
                .inputTextKey("input")
                .categories(List.of("售后", "技术支持", "投诉", "咨询"))
                .classificationInstructions(
                        List.of("请将用户诉求分类到最合适的类别"))
                .build();
        graph.addNode("classify", AsyncNodeAction.node_async(qcNode));

        // —— 4. Knowledge Retrieval ——
        KnowledgeRetrievalNode krNode = KnowledgeRetrievalNode.builder()
                .userPromptKey("input")
                .vectorStore(vectorStore)
                .topK(5)
                .similarityThreshold(0.5)
                .enableRanker(false)
                .build();
        graph.addNode("retrieveDocs", AsyncNodeAction.node_async(krNode));

        // —— 5. List Operate ——
//        ListOperatorNode<ListOperatorNode.StringElement> listOp = ListOperatorNode.<ListOperatorNode.StringElement>builder()
//                .inputTextKey("input")
//                .outputTextKey("filtered_docs")
//                .filter(e -> e.contains("significant"))     // 保留带“重要”关键字的文档
//                .limitNumber(5L)
//                .elementClassType(ListOperatorNode.StringElement.class)
//                .build();
//        graph.addNode("filterDocs", AsyncNodeAction.node_async(listOp));

        // —— 6. call http endpoint ——
        HttpNode httpNode = HttpNode.builder()
                .webClient(WebClient.create())
                .method(HttpMethod.POST)
                .url("http://localhost:8080/api/graph/mock/http")
                .body(HttpNode.HttpRequestNodeBody.from(
                        "{\"ticketId\":\"${ticketId}\",\"category\":\"${classifier_output}\"}"
                ))
                .outputKey("http_response")
                .build();
        graph.addNode("syncTicket", AsyncNodeAction.node_async(httpNode));

        // —— 7. call LLM ——
        LlmNode llmNode = LlmNode.builder()
                .chatClient(chatClient)
                .systemPromptTemplate("你是客服助手，请基于以下信息撰写回复：")
                .userPromptTemplateKey("input")
                .messagesKey("filtered_docs")
                .outputKey("llm_response")
                .build();
        graph.addNode("invokeLLM", AsyncNodeAction.node_async(llmNode));

        // —— 8. Perform a tool call (optional) ——
        ToolNode toolNode = ToolNode.builder()
                .llmResponseKey("llm_response")
                .outputKey("tool_result")
                .toolCallbackResolver(toolCallbackResolver)
                .toolNames(List.of("sendEmail", "updateCRM"))
                .build();
        graph.addNode("invokeTool", AsyncNodeAction.node_async(toolNode));

        // —— 9. human callback ——
        HumanNode humanNode = new HumanNode(
                "conditioned",
                st -> /* 当 tool_result 包含 “ERROR” 时中断交给人工 */
                        st.value("tool_result").map(r -> r.toString().contains("ERROR")).orElse(false),
                /* 审核通过后把最终内容写入 answer */
                st -> Map.of("answer", st.value("tool_result").orElse("").toString())
        );
        graph.addNode("humanReview", AsyncNodeAction.node_async(humanNode));

        // —— 10. end print (this node need to defined in ssa)——
        AnswerNode ansNode = AnswerNode.builder()
                .answer("{{answer}}")
                .build();
        graph.addNode("finalAnswer", AsyncNodeAction.node_async(ansNode));

        graph
                .addEdge(START, "extractDocs")
                .addEdge("extractDocs", "parseParams")
                .addEdge("parseParams", "classify")
                .addEdge("classify", "retrieveDocs")
                .addEdge("retrieveDocs", "syncTicket")
//                .addEdge("filterDocs", "syncTicket")
                .addEdge("syncTicket", "invokeLLM")
                .addEdge("invokeLLM", "invokeTool")
                .addEdge("invokeTool", "humanReview")
                .addEdge("humanReview", "finalAnswer")
                .addEdge("finalAnswer", END);

        return graph.compile();
    }
}
