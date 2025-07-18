package com.alibaba.cloud.ai.example.mcp.streamable.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class McpConfig {
    @Value("${spring.ai.mcp.client.streamable.connections.server1.url}")
    private String mcpServerUrl;

    @Bean
    public WebClientStreamableHttpTransport mcpTransport(ObjectMapper objectMapper) {
        return WebClientStreamableHttpTransport.builder(WebClient.builder())
                .endpoint(mcpServerUrl)
                .resumableStreams(true)
                .objectMapper(objectMapper)
                .openConnectionOnStartup(true)
                .build();
    }

    @Bean
    public McpAsyncClient mcpAsyncClient(WebClientStreamableHttpTransport transport) {
        return McpClient.async(transport).build();
    }

    @Bean
    public McpSchema.Tool startNotificationTool() {
        // 与py server list_tools保持一致，schema直接用json字符串
        String inputSchema = """
                    {
                      "type": "object",
                      "required": ["interval", "count", "caller"],
                      "properties": {
                        "interval": { "type": "number", "description": "Interval between notifications in seconds" },
                        "count": { "type": "number", "description": "Number of notifications to send" },
                        "caller": { "type": "string", "description": "Identifier of the caller to include in notifications" }
                      }
                    }
                """;
        return McpSchema.Tool.builder()
                .name("start-notification-stream")
                .description("Sends a stream of notifications with configurable count and interval")
                .inputSchema(inputSchema)
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
