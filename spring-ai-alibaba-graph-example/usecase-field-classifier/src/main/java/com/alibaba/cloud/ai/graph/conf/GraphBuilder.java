package com.alibaba.cloud.ai.graph.conf;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.code.CodeExecutor;
import com.alibaba.cloud.ai.graph.node.code.CodeExecutorNodeAction;
import com.alibaba.cloud.ai.graph.node.code.LocalCommandlineCodeExecutor;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

@Component
public class GraphBuilder {


    @Bean
    public CompiledGraph buildGraph(ChatModel chatModel, CodeExecutionConfig codeExecutionConfig, CodeExecutor codeExecutor) throws GraphStateException {
        ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();

        // new stateGraph
        StateGraph stateGraph = new StateGraph(() -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("args1", (o1, o2) -> o2);
            strategies.put("args2", (o1, o2) -> o2);
            return strategies;
        });

        // —— CodeNode [1733195173395] ——
        CodeExecutorNodeAction codeNode1 = CodeExecutorNodeAction.builder().codeExecutor(codeExecutor) // 注入的 CodeExecutor Bean
                .codeLanguage("java")
                .code("""
                        package com.example.scripts;
                        
                        import java.util.HashMap;
                        import java.util.Map;
                        
                        public class Script {
                            /**
                             * 约定：CodeExecutorNodeAction 会把 params.values()
                             * 按顺序当作 args 传入。
                             */
                            public static Map<String, Object> main(String[] args) {
                                String a = args[0];
                                String b = args[1];
                                Map<String, Object> result = new HashMap<>();
                                result.put("result", a + b);
                                return result;
                            }
                        }
                        """)
                .config(codeExecutionConfig) // 注入的 CodeExecutionConfig Bean
                .params(Map.of("arg1", "1", "arg2", "2")).outputKey("codeNode1_output").build();
        stateGraph.addNode("codeNode1", AsyncNodeAction.node_async(codeNode1));

        // add edges
        stateGraph.addEdge(START, "codeNode1");
        stateGraph.addEdge("codeNode1", END);
        return stateGraph.compile();
    }

    @Bean
    public Path tempDir() throws IOException {
        Path tempDir = Files.createTempDirectory("code-execution-workdir-");
        tempDir.toFile().deleteOnExit();
        return tempDir;
    }

    @Bean
    public CodeExecutionConfig codeExecutionConfig(Path tempDir) {
        return new CodeExecutionConfig().setWorkDir(tempDir.toString());
    }

    @Bean
    public CodeExecutor codeGenerator() {
        return new LocalCommandlineCodeExecutor();
    }
}
