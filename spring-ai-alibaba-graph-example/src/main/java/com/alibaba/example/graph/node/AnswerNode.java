package com.alibaba.example.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将 AnswerNodeData 中的模板渲染为最终的 answer 字符串。
 * <p>
 * 支持在模板中使用 {{varName}} 占位，从全局状态中读取对应的值替换。
 * </p>
 */
public class AnswerNode implements NodeAction {

    /** 输出 key，固定为 "answer" */
    public static final String OUTPUT_KEY = "answer";

    /** 模板字符串，例如 "The result is {{invokeLLM}}" */
    private final String answerTemplate;

    private AnswerNode(String answerTemplate) {
        this.answerTemplate = answerTemplate;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        // 简单的 {{var}} 替换
        String rendered = this.answerTemplate;
        Pattern p = Pattern.compile("\\{\\{(.+?)}}");
        Matcher m = p.matcher(rendered);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String var = m.group(1).trim();
            String val = Optional.ofNullable(state.value(var).orElse("")).map(Object::toString).orElse("");
            m.appendReplacement(sb, Matcher.quoteReplacement(val));
        }
        m.appendTail(sb);

        Map<String, Object> out = new HashMap<>();
        out.put(OUTPUT_KEY, sb.toString());
        return out;
    }

    /** 构造器 */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String answerTemplate;

        /**
         * 设置 answer 模板
         * @param tpl 支持 {{var}} 占位
         */
        public Builder answer(String tpl) {
            this.answerTemplate = tpl;
            return this;
        }

        public AnswerNode build() {
            return new AnswerNode(this.answerTemplate);
        }
    }
}
