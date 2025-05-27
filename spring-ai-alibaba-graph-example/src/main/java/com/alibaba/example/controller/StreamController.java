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
    private final CompiledGraph graph;

    public StreamController(@Qualifier("streamGraph") StateGraph g) throws GraphStateException {
        this.graph = g.compile();
    }

    @GetMapping(path = "/write", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String text) {
        var inputs = Map.<String,Object>of("original_text", text);
        var cfg    = RunnableConfig.builder()
                .streamMode(CompiledGraph.StreamMode.VALUES)
                .build();

        AsyncGenerator<NodeOutput> gen = graph.stream(inputs, cfg);

        return Flux.create(sink -> {
            gen.forEachAsync(out -> {
                String id = out.node();                                // :contentReference[oaicite:0]{index=0}
                if ("stream_summarizer".equals(id)) {
                    // StreamingOutput.chunk() carries each LLM chunk
                        String chunk = ((StreamingOutput) out).chunk();    // :contentReference[oaicite:1]{index=1}
                    sink.next("[摘要流] " + chunk);
                }
                else if ("title_generator".equals(id)) {
                    // once title node fires, its result sits in state under "title"
                    String title = (String) out.state().data().get("title");
                    sink.next("[标题] " + title);
                }
            }).whenComplete((v, err) -> {
                if (err != null) sink.error(err);
                else            sink.complete();
            });
        }, FluxSink.OverflowStrategy.BUFFER);
    }
}
