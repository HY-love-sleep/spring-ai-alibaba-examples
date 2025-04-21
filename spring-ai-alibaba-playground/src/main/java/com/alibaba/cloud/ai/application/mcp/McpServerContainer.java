package com.alibaba.cloud.ai.application.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.alibaba.cloud.ai.application.entity.mcp.McpServer;

public class McpServerContainer {

	private static final List<McpServer> mcpServerContainer = new ArrayList<>();

	public static List<McpServer> getAllServers() {
		return new ArrayList<>(mcpServerContainer);
	}

	public static Optional<McpServer> getServerById(String id) {

		return mcpServerContainer.stream()
				.filter(server -> server.getId().equals(id))
				.findFirst();
	}

	public static void addServer(McpServer server) {
		mcpServerContainer.add(server);
	}

	public static boolean removeServerById(String id) {

		return mcpServerContainer.removeIf(server -> server.getId().equals(id));
	}

}
