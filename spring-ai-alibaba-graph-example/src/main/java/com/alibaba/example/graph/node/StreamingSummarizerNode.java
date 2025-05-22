package com.alibaba.example.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.bsc.async.AsyncGenerator;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class StreamingSummarizerNode implements NodeAction {

    private final ChatClient client;

    public StreamingSummarizerNode(ChatClient client) {
        this.client = client;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String text = (String) state.value("original_text").orElse("");
        String prompt = "请帮我总结：" + text;

        Flux<String> flux = client.prompt()
                .user(prompt)
                .stream()
                .content();

        AsyncGenerator<StreamingOutput> gen = new AsyncGenerator<>() {
            final Iterator<String> it = flux.toIterable().iterator();

            @Override
            public Data<StreamingOutput> next() {
                if (!it.hasNext()) {
                    return Data.done();
                }
                String chunk = it.next();
                return Data.of(
                        CompletableFuture.completedFuture(
                                new StreamingOutput(chunk, "summarizer", state)
                        )
                );
            }
        };

        return Map.of("summary_stream", gen);
    }
}

