# Spring AI MCP + Nacos 示例项目

本项目是一个基于 [spring-ai-alibaba-mcp-nacos](https://github.com/spring-projects/spring-ai-alibaba) 的简单示例，展示如何将
MCP Server 注册到 Nacos 中，并通过注解式工具（Tool）提供服务。

## 🧩 主要依赖

```xml
<!-- MCP Nacos 注册 -->
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-mcp-nacos</artifactId>
    <version>1.0.0-M6.2-SNAPSHOT</version>
</dependency>

        <!-- MCP Server (WebMVC) -->
<dependency>
<groupId>org.springframework.ai</groupId>
<artifactId>spring-ai-mcp-server-webmvc-spring-boot-starter</artifactId>
<version>1.0.0-M6</version>
</dependency>
```

---

## 🚀 快速开始

### 1. 启动 Nacos 服务

请确保你已经本地或远程启动了 [Nacos 服务器]

### 2. 配置 application.yml

```yaml
spring:
  application:
    name: spring-ai-alibaba-nacos-mcp-example
  ai:
    mcp:
      server:
        name: webmvc-mcp-server
        version: 1.0.0
        type: SYNC
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-max-latest

    alibaba:
      mcp:
        nacos:
          enabled: true
          server-addr:                 # 替换为你的 Nacos 地址
          service-namespace: public    # Nacos 命名空间 ID
          service-group: DEFAULT_GROUP
          username:
          password:

server:
  port: 8080

```

### 3. 启动应用

```bash
mvn spring-boot:run
```

或使用 IDE 运行 `SpringAiMcpApplication.java`。

---

## 🔧 工具服务示例

```java

@Service
public class WeatherService {

    @Tool(description = "Get weather information by city name")
    public String getWeather(String cityName) {
        return "Sunny in " + cityName;
    }
}
```

该服务将注册为 MCP 工具，发布到 Nacos 并被其他 MCP 客户端识别。

---

## 📡 Nacos 注册效果

- MCP Server 信息注册到：

  ```
  配置中心:
  └── nacos-default-mcp 命名空间
      ├── webmvc-mcp-server-mcp-server.json
      └── webmvc-mcp-server-mcp-tools.json
  ```
  ![img_1.png](img_1.png)
- 服务实例注册到：

  ```
  服务发现:
  └── <service-namespace> 命名空间
      └── <service-group> 组
          └── webmvc-mcp-server-mcp-service
  ```
  ![img.png](img.png)

---

## 🚧 后续开发计划（TODO）

本项目目前为最小可运行示例，仅实现了：

- [x] 注册一个基础 MCP Tool（WeatherService）
- [x] 将 MCP Server 注册到 Nacos 服务中心
- [x] 启用配置中心发布 server/tools 元信息（支持热更新）

后续可扩展方向：

- [ ] 添加多个 Tool 示例（如 LLM 接口、数据库查询工具）
- [ ] 支持动态启用/禁用工具（通过 Nacos 修改 toolsMeta）
- [ ] 引入客户端模拟工具调用流程（基于 SSE 协议）
- [ ] 配置中心同步工具变更后的热刷新能力
- [ ] 支持 WebFlux + Reactive MCP Server 的版本

## 📎 参考资料

- [Spring AI Alibaba 相关代码](https://github.com/alibaba/spring-ai-alibaba/tree/main/spring-ai-alibaba-mcp/spring-ai-alibaba-mcp-nacos)

---
