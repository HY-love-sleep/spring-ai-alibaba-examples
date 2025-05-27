package com.alibaba.example.graph.conf;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.bsc.async.AsyncGenerator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;


@Configuration
public class StreamGraphConfiguration {

    @Bean
    public StateGraph streamGraph(ChatModel chatModel) throws GraphStateException {
        ChatClient client = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();

        // 1) register your state keys
        OverAllStateFactory stateFactory = () -> {
            OverAllState s = new OverAllState();
            s.registerKeyAndStrategy("original_text", new ReplaceStrategy());
            s.registerKeyAndStrategy("summary_stream", new AppendStrategy());
            s.registerKeyAndStrategy("title", new ReplaceStrategy());
            return s;
        };

        return new StateGraph("Streaming Demo", stateFactory)

                // 2) streaming summarizer node emits Flux<String> as AsyncGenerator<StreamingOutput>
                .addNode("stream_summarizer",
                        AsyncNodeActionWithConfig.node_async((state, cfg) -> {
                            String text = (String)state.value("original_text").orElse("");
                            Flux<String> flux = client.prompt()
                                    .user("请总结：" + text)
                                    .stream()
                                    .content();

                            AsyncGenerator<StreamingOutput> gen = new AsyncGenerator<>() {
                                Iterator<String> it = flux.toIterable().iterator();
                                @Override
                                public Data<StreamingOutput> next() {
                                    if (!it.hasNext()) return Data.done();
                                    return Data.of(new StreamingOutput(it.next(),
                                            "stream_summarizer",
                                            state));
                                }
                            };
                            // append all chunks into summary_stream list
                            return Map.of("summary_stream", gen);
                        })
                )

                // 3) once streaming is done, run your normal title generator
                .addNode("title_generator",
                        AsyncNodeActionWithConfig.node_async((state, cfg) -> {
                            // pull full summary back out of state
                            @SuppressWarnings("unchecked")
                            List<String> pieces = (List<String>)state.value("summary_stream").orElse(List.of());
                            String full = String.join("", pieces);
                            ChatResponse r = client.prompt("给下面内容起标题：" + full)
                                    .call()
                                    .chatResponse();
                            return Map.of("title", Optional.ofNullable(r.getResult().getOutput().getText()).orElse("my title"));
                        })
                )

                // 4) wire the edges: START → stream_summarizer → title_generator → END
                .addEdge(START,          "stream_summarizer")
                .addEdge("stream_summarizer",       "title_generator")
                .addEdge("title_generator",         StateGraph.END);
    }
}
