package com.alibaba.ai.examples.controller.client;

import java.util.Set;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@RestController
@RequestMapping("/more-model-chat-client")
public class MoreModelChatClientController {

	private final Set<String> modelList = Set.of(
			"deepseek-r1",
			"deepseek-v3",
			"qwen-plus",
			"qwen-max"
	);

	private final ChatClient chatClient;

	public MoreModelChatClientController(
			@Qualifier("dashscopeChatModel") DashScopeChatModel chatModel
	) {

		// 构建 chatClient
		this.chatClient = ChatClient.builder(chatModel).build();
	}

	@GetMapping
	public Flux<String> stream(
			@RequestParam("prompt") String prompt,
			@RequestHeader(value = "models", required = false) String models
	) {

		if (!modelList.contains(models)) {

			return Flux.just("model not exist");
		}

		return chatClient.prompt(prompt)
				.options(DashScopeChatOptions.builder()
						.withModel(models)
						.build()
				).stream()
				.content();
	}

}
