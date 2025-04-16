import React from "react";
import { Components } from "react-markdown";

export const getMarkdownRenderConfig = (
  styles: Record<string, string>
): Components => {
  return {
    blockquote: ({ children }) => (
      <blockquote className={styles.thinkBlock}>{children}</blockquote>
    ),
    code: ({ children, className }) => (
      <code className={styles.codeInline}>{children}</code>
    ),
    pre: ({ children }) => <pre className={styles.codeBlock}>{children}</pre>,
  };
};

// 缓存防止重复
const processedContentCache = new Map<string, string>();
export const getSseTagProcessor = () => {
  let isProcessing = false;
  const sseTagProcessor = (content: string, timestamp: string) => {
    try {
      isProcessing = true;
      const cachedResult = processedContentCache.get(timestamp);
      if (cachedResult) {
        return cachedResult;
      }

      let result = content;
      const thinkRegex = /<think>([\s\S]*?)<\/think>/g;
      const matches = result.matchAll(thinkRegex);

      const thinkContents: string[] = [];
      let lastIndex = 0;
      let processedResult = "";

      for (const match of matches) {
        const [fullMatch, thinkContent] = match;
        processedResult += result.slice(lastIndex, match.index);
        thinkContents.push(thinkContent.trim());
        lastIndex = (match.index || 0) + fullMatch.length;
      }

      // 添加剩余内容
      processedResult += result.slice(lastIndex);

      // 如果有 think 内容，将它们合并并添加引用符号
      if (thinkContents.length > 0) {
        const combinedThinkContent = thinkContents.join("");
        processedResult = `> ${combinedThinkContent}\n${processedResult}`;
      }
      processedContentCache.set(timestamp, processedResult);

      return processedResult;
    } catch (err) {
      console.error("Failed to process content:", err);
      return content;
    } finally {
      isProcessing = false;
    }
  };

  return sseTagProcessor;
};
