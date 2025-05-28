package com.alibaba.example.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final CompiledGraph graph;

    public GraphController(CompiledGraph graph) {
        this.graph = graph;
    }

    /**
     * 接口：执行图，返回所有状态变量
     * POST /api/graph/invoke
     * Content-Type: application/json
     * Body: { "input": "用户的原始文本" }
     */
    @PostMapping("/invoke")
    public ResponseEntity<Map<String, Object>> invoke(
            @RequestBody Map<String, Object> inputs) throws GraphStateException {

        // 执行图
        var resultFuture = graph.invoke(inputs);

        return ResponseEntity.ok(resultFuture.get().data());
    }

    @PostMapping("/mock/http")
    public ResponseEntity<String> mock(@RequestParam("input") String input) {
        return ResponseEntity.ok("httpNode mock:" + input);
    }
}
