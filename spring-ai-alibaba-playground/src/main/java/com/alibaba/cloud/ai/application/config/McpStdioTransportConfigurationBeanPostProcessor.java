/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.application.config;

import java.io.IOException;
import java.util.Map;

import com.alibaba.cloud.ai.application.entity.mcp.McpServerConfig;
import com.alibaba.cloud.ai.application.exception.SAAAppException;
import com.alibaba.cloud.ai.application.mcp.McpServerUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.autoconfigure.mcp.client.StdioTransportAutoConfiguration;
import org.springframework.ai.autoconfigure.mcp.client.properties.McpStdioClientProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import static com.alibaba.cloud.ai.application.mcp.McpServerUtils.getMcpLibsAbsPath;

/**
 * @author brianxiadong
 * @author yuluo
 *
 * MCP 配置增强，使用更友好的 yaml 语义定义，
 * 处理 server 中 jar 的路径问题。
 */

@Component
public class McpStdioTransportConfigurationBeanPostProcessor implements BeanPostProcessor {

	private static final Logger logger = LoggerFactory.getLogger(McpStdioTransportConfigurationBeanPostProcessor.class);

	private final ObjectMapper objectMapper;

	private final McpStdioClientProperties mcpStdioClientProperties;

	public McpStdioTransportConfigurationBeanPostProcessor(
			ObjectMapper objectMapper,
			McpStdioClientProperties mcpStdioClientProperties
	) {
		this.objectMapper = objectMapper;
		this.mcpStdioClientProperties = mcpStdioClientProperties;
	}

	@Override
	public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {

		if (bean instanceof StdioTransportAutoConfiguration) {

			logger.debug("增强 McpStdioTransportConfiguration bean start: {}", beanName);

			McpServerConfig mcpServerConfig;
			try {
				mcpServerConfig = McpServerUtils.getMcpServerConfig();

				// Handle the jar relative path issue in the configuration file.
				for (Map.Entry<String, McpStdioClientProperties.Parameters> entry : mcpServerConfig.getMcpServers()
						.entrySet()) {

					if (entry.getValue() != null && entry.getValue().command().startsWith("java")) {

						McpStdioClientProperties.Parameters serverConfig = entry.getValue();
						String oldMcpLibsPath = McpServerUtils.getLibsPath(serverConfig.args());
						String rewriteMcpLibsAbsPath = getMcpLibsAbsPath(McpServerUtils.getLibsPath(serverConfig.args()));
						if (rewriteMcpLibsAbsPath != null) {
							serverConfig.args().remove(oldMcpLibsPath);
							serverConfig.args().add(rewriteMcpLibsAbsPath);
						}
					}
				}

				String msc = objectMapper.writeValueAsString(mcpServerConfig);
				logger.debug("Registry McpServer config: {}", msc);

				// write mcp client
				mcpStdioClientProperties.setServersConfiguration(new ByteArrayResource(msc.getBytes()));
				((StdioTransportAutoConfiguration) bean).stdioTransports(this.mcpStdioClientProperties);
			}
			catch (IOException e) {
				throw new SAAAppException(e.getMessage());
			}

			logger.debug("增强 McpStdioTransportConfiguration bean end: {}", beanName);
		}

		return bean;
	}

}
