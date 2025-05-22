/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author yHong
 */

package com.alibaba.example.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.bsc.async.AsyncGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Map;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/4/24 16:07
 */
@RestController
@RequestMapping("/write")
public class WritingAssistantController {

	private final CompiledGraph compiledGraph;

	@Autowired
	public WritingAssistantController(@Qualifier("writingAssistantGraph") StateGraph writingAssistantGraph)
			throws GraphStateException {
		this.compiledGraph = writingAssistantGraph.compile();
	}

	/**
	 * 调用写作助手流程图 示例请求：GET /write?text=今天我去了西湖，天气特别好，感觉特别开心
	 */
	@GetMapping
	public Map<String, Object> write(@RequestParam("text") String inputText) {
		var resultFuture = compiledGraph.invoke(Map.of("original_text", inputText));
		var result = resultFuture.get();
		return result.data();
	}

	@GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> writeStream(@RequestParam("text") String input) {
		Map<String, Object> inputs = Map.of("original_text", input);

		RunnableConfig cfg = RunnableConfig.builder()
				.streamMode(CompiledGraph.StreamMode.VALUES)
				.build();

		AsyncGenerator<NodeOutput> gen = compiledGraph.stream(inputs, cfg);

		return Flux.create(sink -> {
			gen.forEachAsync(nodeOut -> {
				if ("summarizer".equals(nodeOut.node())) {
					String chunk = ((StreamingOutput) nodeOut).chunk();
					sink.next(chunk);
				}
			}).whenComplete((v, err) -> {
				if (err != null) sink.error(err);
				else sink.complete();
			});
		}, FluxSink.OverflowStrategy.BUFFER);
	}

}
