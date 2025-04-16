/*
 * Copyright 2024 the original author or authors.
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
 */

package com.alibaba.cloud.ai.example.dcg.agent;

import com.alibaba.cloud.ai.example.dcg.tools.ClftTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * Description: 数据分类分级智能体服务类
 * Author: yhong
 * Date: 2025/4/12
 */
@Service
public class ClassificationAssistant {

	private final ChatClient chatClient;

	public ClassificationAssistant(ChatClient.Builder modelBuilder,
								   VectorStore classificationVectorStore,
								   ChatMemory chatMemory,
								   ClftTools clftTools) {

		// 构造 ChatClient，注入 RAG 和 Memory 等 Advisor， 注入functionCall以调用本地方法
		this.chatClient = modelBuilder
				.defaultSystem("""
							您是一个数据安全分类分级助手，您能够支持已有字段的详情查询， 字段分类分级结果入库以及修改字段的分类分级结果， 其余功能将在后续版本中添加，如果用户问的问题不支持请告知详情。
							在已有字段的详情查询， 字段分类分级结果入库以及修改字段的分类分级结果等操作之前， 您应该始终从用户处获取以下信息：字段名称【field_name】
							使用提供的功能获取已有字段的详情、字段分类分级结果入库以及修改字段的分类分级结果。如果需要，您可以调用相应函数辅助完成。
							当用户要求保存或更新字段分类分级时，请调用本地函数进行处理。您可以使用如下功能：
						         - 获取字段信息：[getFieldInfo]
						         - 保存字段分类分级结果：[saveFieldCategoryGrade] 在保存时不用传入字段ID，系统将自动记录。
						         - 修改字段分类分级结果：[updateFieldCategoryGrade]
							请根据用户输入的字段名和下方提供的字段分类知识，判断该字段属于哪个分类路径，分级是多少，并简要说明理由。
							请使用如下格式输出：
								 字段名：...
								 分类路径：一级 > 二级 > 三级 > 四级
								 分级：第X级
								 理由：...
							如果用户需要更新或者插入分类分级后的字段详情， 请调用合适的函数将字段名、分类分级结果以及理由更新或插入到数据库中。
						""")
				.defaultAdvisors(
						// Chat Memory
						new PromptChatMemoryAdvisor(chatMemory),
						new QuestionAnswerAdvisor(
								// RAG
								classificationVectorStore,
								SearchRequest.builder().topK(5).similarityThresholdAll().build()
						),
						new SimpleLoggerAdvisor()

				)
				// 注入functionCall, 提供大模型可调用的本地方法【调用本类的classify和streamClassify方法时注释掉tools调用】
//				.defaultTools(clftTools)
				.defaultFunctions("getFieldInfo", "saveFieldCategoryGrade", "updateFieldCategoryGrade")
				.build();
	}

	/**
	 * 字段分类与分级推理方法
	 * @param fieldName 用户输入的字段名，如“专利交底书”
	 * @return 返回模型推理结果文本
	 */
	public String classify(String fieldName, String chatId) {
		// 调用 ChatClient 进行推理，并返回内容
		return chatClient.prompt()
				.user(fieldName)
				// 设置advisor参数， 记忆使用chatId， 拉取最近的100条记录
				.advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
				.call()
				.content();
	}

	public Flux<String> streamClassify(String fieldName, String chatId) {
		return chatClient.prompt()
				.user(fieldName)
				// 设置advisor参数， 记忆使用chatId， 拉取最近的100条记录
				.advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
				.stream()
				.content();
	}

	/**
	 * 通过tools调用， 处理分类分级与数据库交互
	 * @param input
	 * @param chatId
	 * @return
	 */
	public String classifyWithFunc(String input, String chatId) {
		return chatClient.prompt()
				.user(input)
				.advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
				.call()
				.content();
	}


}
