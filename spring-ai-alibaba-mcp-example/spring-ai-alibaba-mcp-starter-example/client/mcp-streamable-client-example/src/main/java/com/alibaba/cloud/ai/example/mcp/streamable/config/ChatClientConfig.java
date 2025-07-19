package com.alibaba.cloud.ai.example.mcp.streamable.config;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.AsyncMcpToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {
    @Bean
    public AsyncMcpToolCallback mcpToolCallback(
            McpAsyncClient mcpAsyncClient,
            McpSchema.Tool startNotificationTool
    ) {
        return new AsyncMcpToolCallback(mcpAsyncClient, startNotificationTool);
    }
}
