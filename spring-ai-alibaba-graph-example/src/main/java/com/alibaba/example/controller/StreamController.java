package com.alibaba.example.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.bsc.async.AsyncGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Map;

@RestController
@RequestMapping("/stream")
public class StreamController {

    private final CompiledGraph compiledGraph;

    public StreamController(@Qualifier("streamGraph") StateGraph g)
            throws GraphStateException {
        this.compiledGraph = g.compile();
    }

    @GetMapping(path = "/write", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam("text") String input) {
        Map<String,Object> inputs = Map.of("original_text", input);

        RunnableConfig cfg = RunnableConfig.builder()
                .streamMode(CompiledGraph.StreamMode.VALUES)
                .build();

        AsyncGenerator<NodeOutput> gen = compiledGraph.stream(inputs, cfg);

        return Flux.create(sink -> {
            gen.forEachAsync(nodeOut -> {
                // only when it really is a chunk:
                if (nodeOut instanceof StreamingOutput so
                        && "stream_summarizer".equals(so.node())) {
                    sink.next(so.chunk());
                }
            }).whenComplete((v, err) -> {
                if (err != null) sink.error(err);
                else           sink.complete();
            });
        }, FluxSink.OverflowStrategy.BUFFER);
    }
}
