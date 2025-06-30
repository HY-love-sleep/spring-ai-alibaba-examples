package com.alibaba.cloud.ai.graph.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.HashMap;

@RestController("/run")
public class GraphRunController {

    private CompiledGraph graph;

    public GraphRunController(@Qualifier("buildGraph") CompiledGraph graph) {
        this.graph = graph;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<NodeOutput> stream(@RequestBody Map<String, Object> inputs) throws Exception {
        AsyncGenerator<NodeOutput> nodeOutputs = graph.stream(inputs);
        return Flux.fromStream(nodeOutputs.stream());
    }

    @PostMapping(value = "/invoke")
    public OverAllState invoke(@RequestBody Map<String, Object> inputs) throws Exception {
        return graph.invoke(inputs).orElse(null);
    }

    @PostMapping(value = "/start")
    public Map<String, Object> startInvoke(@RequestBody Map<String, Object> inputs) throws Exception {
        Map<String, Object> startInputs = new HashMap<>();
        startInputs.put("args1", inputs.get("args1")); // null
        startInputs.put("args2", inputs.get("args2")); // null
        return graph.invoke(startInputs).get().data();

    }

}
