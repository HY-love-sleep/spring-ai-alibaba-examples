package com.alibaba.cloud.ai.example.dcg.tools;

import com.alibaba.cloud.ai.example.dcg.server.entity.Field;
import com.alibaba.cloud.ai.example.dcg.server.service.IFieldService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/4/15 16:45
 */
@Configuration
@Slf4j
public class ClftTools {
    private final IFieldService fieldService;
    public ClftTools(IFieldService fieldService) {
        this.fieldService = fieldService;
    }

    @Bean
    @Description("根据fieldName获取字段详细信息")
    @Tool(description = "根据fieldName获取字段详细信息")
    public Function<String, Field> getFieldInfo() {
        log.info("=========call getFieldInfo============");
        return fieldService::getFieldByName;
    }

    @Bean
    @Description("保存字段分类分级结果")
    @Tool(description = "保存字段分类分级结果")
    public Function<Field, String> saveFieldCategoryGrade() {
        log.info("=========call saveFieldCategoryGrade============");
        return field -> {
            try {
                fieldService.save(field);
                return "保存成功";
            } catch (Exception e) {
                log.error("保存失败：", e);
                return "保存失败";
            }
        };
    }

    @Bean
    @Description("修改字段分类分级结果")
    @Tool(description = "修改字段分类分级结果")
    public Function<Field, String> updateFieldCategoryGrade() {
        log.info("=========call updateFieldCategoryGrade============");
        return field -> {
            try {
                fieldService.saveOrUpdate(field);
                return "修改成功";
            } catch (Exception e) {
                log.error("修改失败：", e);
                return "修改失败";
            }
        };
    }

}