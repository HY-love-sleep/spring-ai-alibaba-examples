package com.alibaba.example.graph.conf;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.bsc.async.AsyncGenerator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.Iterator;
import java.util.Map;


@Configuration
public class StreamGraphConfiguration {

    @Bean
    public StateGraph streamGraph(ChatModel chatModel) throws GraphStateException {
        ChatClient client = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();

        OverAllStateFactory stateFactory = () -> {
            OverAllState s = new OverAllState();
            s.registerKeyAndStrategy("original_text", new ReplaceStrategy());
            s.registerKeyAndStrategy("summary_stream", new AppendStrategy());
            return s;
        };

        return new StateGraph("Streaming Demo", stateFactory)
                .addNode("stream_summarizer",
                        // use node_async
                        AsyncNodeActionWithConfig.node_async((state, cfg) -> {
                            String text = (String) state.value("original_text").orElse("");
                            String prompt = "请对以下中文文本进行简洁明了的摘要：" + text;

                            Flux<String> flux = client.prompt()
                                    .user(prompt)
                                    .stream()
                                    .content();

                            AsyncGenerator<StreamingOutput> gen = new AsyncGenerator<>() {
                                Iterator<String> it = flux.toIterable().iterator();
                                @Override
                                public Data<StreamingOutput> next() {
                                    if (!it.hasNext()) {
                                        return Data.done();
                                    }
                                    String chunk = it.next();
                                    return Data.of(new StreamingOutput(chunk,
                                            "stream_summarizer",
                                            state));
                                }
                            };

                            return Map.of("summary_stream", gen);
                        })
                )
                .addEdge(StateGraph.START,          "stream_summarizer")
                .addEdge("stream_summarizer", StateGraph.END);
    }
}
