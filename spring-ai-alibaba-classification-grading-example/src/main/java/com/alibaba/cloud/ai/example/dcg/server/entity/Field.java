package com.alibaba.cloud.ai.example.dcg.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author HY-love-sleep
 * @since 2025-04-15
 */
public class Field implements Serializable {

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 字段名
     */
    private String fieldName;

    /**
     * 级别
     */
    private Integer level;

    /**
     * 类别
     */
    private String classification;

    /**
     * 推理过程
     */
    private String reasoning;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    @Override
    public String toString() {
        return "Field{" +
        ", id = " + id +
        ", fieldName = " + fieldName +
        ", level = " + level +
        ", classification = " + classification +
        ", reasoning = " + reasoning +
        "}";
    }
}
