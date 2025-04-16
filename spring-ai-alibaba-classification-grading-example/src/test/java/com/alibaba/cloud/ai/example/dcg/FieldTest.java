package com.alibaba.cloud.ai.example.dcg;

import com.alibaba.cloud.ai.example.dcg.server.entity.Field;
import com.alibaba.cloud.ai.example.dcg.server.service.IFieldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/4/15 18:55
 */
@SpringBootTest
public class FieldTest {
    @Autowired
    private IFieldService fieldService;
    @Test
    void test() {
        Field field = new Field();
        field.setFieldName("中国");
        field.setLevel(2);
        field.setClassification("1122");
        field.setReasoning("1122");
        fieldService.saveOrUpdate(field);
    }
}